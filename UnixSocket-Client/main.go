// goclient — CLI for a Java-based command system over Unix Domain Sockets.
//
// Invocation pattern:
//   goclient [client-options] <command> [command-args...]
//
// Example:
//   goclient --socket-path /tmp/stormflood-controlcenter.sock unit info abc-123
//
// As soon as the first non-option argument is seen, that token and everything
// after it is treated as the server command — even if further "--flags" follow,
// they are forwarded unparsed to the server. Use "--" to explicitly terminate
// the client-option section.
//
// Installing tab completion:
//   goclient completion bash  > /etc/bash_completion.d/goclient   # system-wide
//   goclient completion zsh   > "${fpath[1]}/_goclient"
//   goclient completion fish  > ~/.config/fish/completions/goclient.fish

package main

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strings"
	"time"
)

// === Protocol ===

type request struct {
	Op      string   `json:"op"`
	Command string   `json:"command"`
	Tokens  []string `json:"tokens"`
	Partial string   `json:"partial,omitempty"`
}

type response struct {
	Success     bool     `json:"success"`
	Output      string   `json:"output,omitempty"`
	Code        string   `json:"code,omitempty"`
	Message     string   `json:"message,omitempty"`
	Completions []string `json:"completions,omitempty"`
}

// === Client options ===

type optionDef struct {
	long       string
	short      string
	takesValue bool
	desc       string
}

// Add new client options here. If takesValue is true, both
// "--socket-path /path" and "--socket-path=/path" are accepted.
var clientOptions = []optionDef{
	{long: "socket-path", short: "s", takesValue: true, desc: "Path to the Unix Domain Socket"},
	{long: "help", short: "h", takesValue: false, desc: "Show this help"},
}

func findOption(name string) *optionDef {
	for i := range clientOptions {
		o := &clientOptions[i]
		if o.long == name || (o.short != "" && o.short == name) {
			return o
		}
	}
	return nil
}

// === Configuration ===

type config struct {
	socketPath string
	help       bool
}

const envSocket = "STORMFLOOD_SOCKET"

// defaultSocketPath resolves the socket path from the environment.
// Precedence overall (with parseClientArgs):
//   1. explicit --socket-path / -s flag
//   2. $STORMFLOOD_SOCKET environment variable
//
// No filesystem fallback — running multiple control centers in parallel makes
// any implicit default ambiguous, so the user must pick one explicitly.
func defaultSocketPath() string {
	return os.Getenv(envSocket)
}

func defaultConfig() config {
	return config{socketPath: defaultSocketPath()}
}

func applyOption(cfg *config, long, value string) {
	switch long {
	case "socket-path":
		cfg.socketPath = value
	case "help":
		cfg.help = true
	}
}

// === Token parsing ===

// splitOptionToken decomposes an option token, e.g.:
//   "--socket-path=/foo" -> ("socket-path", "/foo", true)
//   "--socket-path"      -> ("socket-path", "",     false)
//   "-s=/foo"            -> ("s",           "/foo", true)
//   "-s"                 -> ("s",           "",     false)
func splitOptionToken(t string) (name, value string, hasInlineValue bool) {
	body := t
	if strings.HasPrefix(t, "--") {
		body = t[2:]
	} else if strings.HasPrefix(t, "-") {
		body = t[1:]
	}
	if eq := strings.IndexByte(body, '='); eq >= 0 {
		return body[:eq], body[eq+1:], true
	}
	return body, "", false
}

// parseClientArgs consumes client options from the start of args until the
// first non-option token. That token and everything after it is the command
// (forwarded unparsed). "--" can be used as an explicit terminator.
func parseClientArgs(args []string) (config, []string, error) {
	cfg := defaultConfig()
	i := 0
	for i < len(args) {
		t := args[i]

		// Non-option (no dash prefix, or just "-") marks the start of the command.
		if !strings.HasPrefix(t, "-") || t == "-" {
			return cfg, args[i:], nil
		}
		// Explicit terminator.
		if t == "--" {
			return cfg, args[i+1:], nil
		}

		name, value, hasInline := splitOptionToken(t)
		spec := findOption(name)
		if spec == nil {
			return cfg, nil, fmt.Errorf("unknown option: %s", t)
		}

		if spec.takesValue {
			if !hasInline {
				if i+1 >= len(args) {
					return cfg, nil, fmt.Errorf("option %s requires a value", t)
				}
				value = args[i+1]
				i += 2
			} else {
				i++
			}
			applyOption(&cfg, spec.long, value)
		} else {
			if hasInline {
				return cfg, nil, fmt.Errorf("option %s does not take a value", t)
			}
			applyOption(&cfg, spec.long, "")
			i++
		}
	}
	return cfg, nil, nil
}

// === Completion parsing ===

type completionState struct {
	cfg            config
	commandTokens  []string   // everything from the command start onward (including the command name itself)
	commandStarted bool       // true once we have left the client-option section
	waitingValue   *optionDef // set when the last token was an option still expecting its value
}

// parseForCompletion is a fault-tolerant variant that identifies the current
// state of tab completion. Unknown options are tolerated (the user may be
// still typing).
func parseForCompletion(tokens []string) completionState {
	st := completionState{cfg: defaultConfig()}
	i := 0
	for i < len(tokens) {
		t := tokens[i]
		if !strings.HasPrefix(t, "-") || t == "-" {
			st.commandStarted = true
			st.commandTokens = tokens[i:]
			return st
		}
		if t == "--" {
			st.commandStarted = true
			st.commandTokens = tokens[i+1:]
			return st
		}

		name, value, hasInline := splitOptionToken(t)
		spec := findOption(name)
		if spec == nil {
			// Unknown option (possibly half-typed) — just skip it.
			i++
			continue
		}

		if spec.takesValue {
			if hasInline {
				applyOption(&st.cfg, spec.long, value)
				i++
			} else {
				if i+1 >= len(tokens) {
					// Last token is an option without its value → user is about to type it.
					st.waitingValue = spec
					return st
				}
				applyOption(&st.cfg, spec.long, tokens[i+1])
				i += 2
			}
		} else {
			applyOption(&st.cfg, spec.long, "")
			i++
		}
	}
	return st
}

// === Network ===

func send(cfg config, req request) (*response, error) {
	conn, err := net.DialTimeout("unix", cfg.socketPath, 2*time.Second)
	if err != nil {
		return nil, err
	}
	defer conn.Close()
	_ = conn.SetDeadline(time.Now().Add(10 * time.Second))

	if err := json.NewEncoder(conn).Encode(req); err != nil {
		return nil, err
	}
	var resp response
	if err := json.NewDecoder(bufio.NewReader(conn)).Decode(&resp); err != nil {
		return nil, err
	}
	return &resp, nil
}

// === Main entry point ===

func main() {
	args := os.Args[1:]

	// Intercept special subcommands BEFORE parsing client options, so users
	// don't have to type any options for "goclient completion bash" etc.
	if len(args) > 0 {
		switch args[0] {
		case "__complete":
			doComplete(args[1:])
			return
		case "completion":
			doCompletionScript(args[1:])
			return
		}
	}

	cfg, commandTokens, err := parseClientArgs(args)
	if err != nil {
		fmt.Fprintln(os.Stderr, "error:", err)
		printHelpShort()
		os.Exit(2)
	}

	if cfg.help {
		printHelp()
		return
	}

	if len(commandTokens) == 0 {
		printHelp()
		os.Exit(2)
	}

	if cfg.socketPath == "" {
		fmt.Fprintf(os.Stderr, "error: no socket path configured — set --socket-path / -s or $%s\n", envSocket)
		os.Exit(2)
	}

	command := commandTokens[0]
	tokens := commandTokens[1:]
	// Ensure JSON emits [] rather than null for an empty token list.
	if tokens == nil {
		tokens = []string{}
	}

	resp, err := send(cfg, request{
		Op:      "execute",
		Command: command,
		Tokens:  tokens,
	})
	if err != nil {
		fmt.Fprintln(os.Stderr, "connection failed:", err)
		os.Exit(2)
	}

	if resp.Output != "" {
		fmt.Print(resp.Output)
		if !strings.HasSuffix(resp.Output, "\n") {
			fmt.Println()
		}
	}
	if !resp.Success {
		if resp.Message != "" {
			if resp.Code != "" {
				fmt.Fprintf(os.Stderr, "%s: %s\n", resp.Code, resp.Message)
			} else {
				fmt.Fprintln(os.Stderr, resp.Message)
			}
		}
		os.Exit(1)
	}
}

// === Completion handler ===

// doComplete is invoked by the shell.
// Invocation pattern:  __complete --partial=<partial> <prev_token_1> <prev_token_2> ...
//
// The --partial= flag approach is used (instead of e.g. "--" as a separator)
// because empty partials (trailing space) would otherwise be handled
// differently across shells — fish in particular drops empty arguments
// during variable expansion.
func doComplete(rawArgs []string) {
	partial := ""
	tokens := []string{}
	for _, a := range rawArgs {
		if strings.HasPrefix(a, "--partial=") {
			partial = a[len("--partial="):]
		} else {
			tokens = append(tokens, a)
		}
	}

	st := parseForCompletion(tokens)

	// Case 1: we're waiting for the value of a client option (e.g. after `--socket-path `).
	// → No suggestions; let the shell fall back to its default completion (e.g. files).
	if st.waitingValue != nil {
		return
	}

	// Case 2: still inside the client-option section.
	if !st.commandStarted {
		switch {
		case strings.HasPrefix(partial, "--"):
			completeLongOptions(partial)
		case strings.HasPrefix(partial, "-") && partial != "-":
			completeShortOptions(partial)
		default:
			// partial is either empty or the beginning of a top-level command.
			// Ask the server for top-level completions — the server should
			// interpret command=="" as "list all root commands".
			askServerForCompletions(st.cfg, "", []string{}, partial)
			// On empty partial, also surface the client options.
			if partial == "" {
				completeLongOptions("--")
			}
		}
		return
	}

	// Case 3: we are inside the command section.
	if len(st.commandTokens) == 0 {
		// "--" was typed with nothing after it → suggest top-level commands.
		askServerForCompletions(st.cfg, "", []string{}, partial)
		return
	}
	command := st.commandTokens[0]
	serverTokens := st.commandTokens[1:]
	if serverTokens == nil {
		serverTokens = []string{}
	}
	askServerForCompletions(st.cfg, command, serverTokens, partial)
}

func completeLongOptions(partial string) {
	for _, o := range clientOptions {
		full := "--" + o.long
		if strings.HasPrefix(full, partial) {
			fmt.Println(full)
		}
	}
}

func completeShortOptions(partial string) {
	for _, o := range clientOptions {
		if o.short == "" {
			continue
		}
		full := "-" + o.short
		if strings.HasPrefix(full, partial) {
			fmt.Println(full)
		}
	}
}

func askServerForCompletions(cfg config, command string, tokens []string, partial string) {
	if cfg.socketPath == "" {
		return // No socket configured → no server-side completions.
	}
	resp, err := send(cfg, request{
		Op:      "complete",
		Command: command,
		Tokens:  tokens,
		Partial: partial,
	})
	if err != nil {
		return // Fail silently during completion, otherwise the shell would spam errors.
	}
	for _, c := range resp.Completions {
		fmt.Println(c)
	}
}

// === Help ===

func printHelp() {
	bin := filepath.Base(os.Args[0])
	fmt.Printf("Usage: %s [options] <command> [args...]\n\n", bin)
	fmt.Println("Client options:")
	for _, o := range clientOptions {
		flag := "    --" + o.long
		if o.short != "" {
			flag = "-" + o.short + ", --" + o.long
		}
		if o.takesValue {
			flag += " <value>"
		}
		fmt.Printf("  %-30s %s\n", flag, o.desc)
	}
	fmt.Println()
	fmt.Println("Special subcommands:")
	fmt.Println("  completion bash|zsh|fish      Print shell tab-completion script")
	fmt.Println()
	fmt.Printf("Example:\n  %s --socket-path /tmp/foo.sock unit info abc-123\n\n", bin)
	fmt.Printf("Socket path resolution (in order):\n")
	fmt.Printf("  1. --socket-path / -s <path>\n")
	fmt.Printf("  2. $%s\n", envSocket)
	fmt.Printf("\nOne of these MUST be set — there is no implicit default.\n")
}

func printHelpShort() {
	bin := filepath.Base(os.Args[0])
	fmt.Fprintf(os.Stderr, "Usage: %s [options] <command> [args...]\n", bin)
	fmt.Fprintf(os.Stderr, "       %s --help  for details\n", bin)
}

// === Completion scripts ===

func doCompletionScript(args []string) {
	if len(args) == 0 {
		fmt.Fprintln(os.Stderr, "usage: completion bash|zsh|fish")
		os.Exit(2)
	}
	bin := filepath.Base(os.Args[0])
	// Bash and Zsh function names may not contain "-".
	funcName := "_" + strings.ReplaceAll(bin, "-", "_") + "_complete"

	var tpl string
	switch args[0] {
	case "bash":
		tpl = bashTemplate
	case "zsh":
		tpl = zshTemplate
	case "fish":
		tpl = fishTemplate
	default:
		fmt.Fprintln(os.Stderr, "unsupported shell:", args[0])
		fmt.Fprintln(os.Stderr, "supported: bash, zsh, fish")
		os.Exit(2)
	}
	out := strings.ReplaceAll(tpl, "__BIN__", bin)
	out = strings.ReplaceAll(out, "__FUNC__", funcName)
	fmt.Print(out)
}

const bashTemplate = `__FUNC__() {
    local IFS=$'\n'
    local cur="${COMP_WORDS[COMP_CWORD]}"
    local prev=()
    if (( COMP_CWORD > 1 )); then
        prev=("${COMP_WORDS[@]:1:COMP_CWORD-1}")
    fi
    local results
    results=$(__BIN__ __complete "--partial=$cur" "${prev[@]}" 2>/dev/null)
    COMPREPLY=( $(compgen -W "$results" -- "$cur") )
}
complete -F __FUNC__ __BIN__
`

const zshTemplate = `#compdef __BIN__
__FUNC__() {
    local cur="${words[CURRENT]}"
    local -a prev
    if (( CURRENT > 1 )); then
        prev=("${(@)words[2,CURRENT-1]}")
    else
        prev=()
    fi
    local -a results
    results=("${(@f)$(__BIN__ __complete "--partial=$cur" "${prev[@]}" 2>/dev/null)}")
    compadd -a results
}
__FUNC__ "$@"
`

const fishTemplate = `function __FUNC__
    set -l prev (commandline -opc)
    set -l partial (commandline -ct)
    if test (count $prev) -gt 0
        set -e prev[1]
    end
    __BIN__ __complete "--partial=$partial" $prev 2>/dev/null
end
complete -c __BIN__ -f -a "(__FUNC__)"
`
