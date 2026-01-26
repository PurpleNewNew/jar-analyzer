/*
 * GPLv3 License
 *
 * Copyright (c) 2022-2026 4ra1n (Jar Analyzer Team)
 *
 * This project is distributed under the GPLv3 license.
 *
 * https://github.com/jar-analyzer/jar-analyzer/blob/master/LICENSE
 */

package tools

import (
	"github.com/mark3labs/mcp-go/server"
	"jar-analyzer-mcp/pkg/log"
)

func RegisterAllTools(s *server.MCPServer) {
	RegisterJarTools(s)
	RegisterSpringTools(s)
	RegisterMethodClassTools(s)
	RegisterCallGraphTools(s)
	RegisterCodeTools(s)
	RegisterResourceTools(s)
	RegisterConfigUsageTools(s)
	RegisterSemanticTools(s)
	RegisterSecurityTools(s)
	RegisterDfsTaintTools(s)
	log.Debug("register core tools")
}

func RegisterAuditFastTools(s *server.MCPServer) {
	RegisterAllTools(s)
	log.Debug("register audit-fast tools")
}

func RegisterGraphLiteTools(s *server.MCPServer) {
	RegisterCallGraphTools(s)
	RegisterSemanticTools(s)
	RegisterJarTools(s)
	RegisterMethodClassTools(s)
	log.Debug("register graph-lite tools")
}

func RegisterScaLeakToolsLine(s *server.MCPServer) {
	RegisterSecurityTools(s)
	log.Debug("register sca-leak tools")
}

func RegisterVulRulesToolsLine(s *server.MCPServer) {
	RegisterSecurityTools(s)
	log.Debug("register vul-rules tools")
}
