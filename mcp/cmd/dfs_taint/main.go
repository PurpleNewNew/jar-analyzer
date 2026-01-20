/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */
package main

import (
	"flag"
	"fmt"
	"net/http"

	"jar-analyzer-mcp/pkg/conf"
	"jar-analyzer-mcp/pkg/log"
	"jar-analyzer-mcp/pkg/tools"

	"github.com/mark3labs/mcp-go/server"
)

const (
	version = "1.2.1"
	name    = "jar-analyzer-mcp-dfs"
)

func main() {
	fmt.Println("     ____.               _____                .__          " +
		"\n" +
		"    |    |____ _______  /  _  \\   ____ _____  |  | ___.__.____" +
		"_____ ___________ \n" +
		"    |    \\__  \\\\_  __ \\/  /_\\  \\ /    \\\\__  \\ |  |<   " +
		" |  |\\___   // __ \\_  __ \\\n" +
		"/\\__|    |/ __ \\|  | \\/    |    \\   |  \\/ __ \\|  |_\\___ " +
		"  | /    /\\  ___/|  | \\/\n" +
		"\\________(____  /__|  \\____|__  /___|  (____  /____/ ____|/__" +
		"____ \\\\___  >__|   \n" +
		"              \\/              \\/     \\/     \\/     \\/     " +
		"       \\/    \\/       ")
	fmt.Println("jar-analyzer-mcp-dfs (https://github.com/jar-analyzer/jar-analyzer)")
    fmt.Printf("version: %s usage: %s\n", version, "[mcp-dfs.exe -port 20035 -url http://127.0.0.1:10032]")

	var debug bool
	var port int
	var mcpAuth bool
	var mcpToken string

	var jarAnalyzerUrl string
	var jarAnAuth bool
	var jarAnToken string

    flag.IntVar(&port, "port", 20035, "port to listen on")
	flag.BoolVar(&mcpAuth, "auth", false, "enable mcp auth")
	flag.StringVar(&mcpToken, "token", "JAR-ANALYZER-MCP-TOKEN", "mcp token")
	flag.BoolVar(&debug, "debug", false, "debug mode")

	// JAR-ANALYZER CONFIG
	flag.StringVar(&jarAnalyzerUrl, "url", "http://127.0.0.1:10032", "Jar Analyzer URL")
	flag.BoolVar(&jarAnAuth, "ja", false, "enable jar-analyzer-api token")
	flag.StringVar(&jarAnToken, "jt", "JAR-ANALYZER-API-TOKEN", "jar-analyzer-api token")

	flag.Parse()

	conf.GlobalPort = port
	conf.McpAuth = mcpAuth
	conf.McpToken = mcpToken

	conf.GlobalJarAnalyzerUrl = jarAnalyzerUrl
	conf.JarAnalyzerAuth = jarAnAuth
	conf.JarAnalyzerToken = jarAnToken

	if debug {
		log.SetLevel(log.DebugLevel)
	} else {
		log.SetLevel(log.InfoLevel)
	}

	fmt.Println("-----------------------------------------------------------")
	fmt.Println("[INFO] Starting Jar Analyzer MCP DFS/Taint Server...")
	fmt.Println("[信息] 正在启动 Jar Analyzer MCP DFS/Taint 服务器...")
	fmt.Println("-----------------------------------------------------------")
	fmt.Printf("[CONF] Listen Port (监听端口): %d\n", port)
	fmt.Printf("[CONF] Backend URL (后端地址): %s\n", jarAnalyzerUrl)
	if mcpAuth {
		fmt.Printf("[CONF] MCP Auth (MCP鉴权): Enabled (开启) [Token: %s]\n", mcpToken)
	} else {
		fmt.Printf("[CONF] MCP Auth (MCP鉴权): Disabled (关闭)\n")
	}

	if jarAnAuth {
		fmt.Printf("[CONF] Backend Auth (后端鉴权): Enabled (开启) [Token: %s]\n", jarAnToken)
	} else {
		fmt.Printf("[CONF] Backend Auth (后端鉴权): Disabled (关闭)\n")
	}

	fmt.Printf("[CONF] Debug Mode (调试模式): %v\n", debug)
	fmt.Println("-----------------------------------------------------------")
	fmt.Println("[HINT] Please ensure Jar Analyzer is running at the backend URL")
	fmt.Println("[提示] 请确保 Jar Analyzer 正在后端地址运行")
	fmt.Println("[HINT] Use an MCP client (like Claude Desktop) to connect to this server")
	fmt.Println("[提示] 使用 MCP 客户端 (如 Claude Desktop) 连接到此服务器")
	fmt.Printf("[HINT] SSE URL: http://localhost:%d/sse\n", port)
	fmt.Printf("[提示] SSE 连接地址: http://localhost:%d/sse\n", port)
	fmt.Printf("[HINT] Streamable HTTP URL: http://localhost:%d/mcp\n", port)
	fmt.Printf("[提示] Streamable HTTP 连接地址: http://localhost:%d/mcp\n", port)
	fmt.Println("-----------------------------------------------------------")

	s := server.NewMCPServer(
		name,
		version,
		server.WithToolCapabilities(false),
		server.WithRecovery(),
	)
	tools.RegisterDfsTools(s)
	tools.RegisterCodeCFRTool(s)
	sseServer := server.NewSSEServer(s)
	streamServer := server.NewStreamableHTTPServer(s)

	mux := http.NewServeMux()
	mux.Handle("/sse", sseServer.SSEHandler())
	mux.Handle("/message", sseServer.MessageHandler())
	mux.Handle("/mcp", streamServer)
	mux.Handle("/mcp/", streamServer)

	httpServer := &http.Server{
		Addr:    fmt.Sprintf(":%d", conf.GlobalPort),
		Handler: mux,
	}
	if err := httpServer.ListenAndServe(); err != nil {
		log.Error(err)
	}
}
