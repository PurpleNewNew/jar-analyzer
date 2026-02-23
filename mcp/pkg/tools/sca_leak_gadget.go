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
	"context"
	"net/url"

	"jar-analyzer-mcp/pkg/conf"
	"jar-analyzer-mcp/pkg/log"
	"jar-analyzer-mcp/pkg/util"

	"github.com/mark3labs/mcp-go/mcp"
	"github.com/mark3labs/mcp-go/server"
)

func RegisterSecurityTools(s *server.MCPServer) {
	registerVulRules(s)
	registerScaLeakGadget(s)
	log.Debug("register security tools")
}

func registerVulRules(s *server.MCPServer) {
	vulRules := mcp.NewTool("vul_rules",
		mcp.WithDescription("List vulnerability rules."),
	)
	s.AddTool(vulRules, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		out, err := util.HTTPGet("/api/security/vul-rules", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	vulSearch := mcp.NewTool("vul_search",
		mcp.WithDescription("Search exploitable vulnerability findings (app-reachable only)."),
		mcp.WithString("name", mcp.Description("Rule names list (optional).")),
		mcp.WithString("level", mcp.Description("Rule level high/medium/low (optional).")),
		mcp.WithString("limit", mcp.Description("Page size (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
	)
	s.AddTool(vulSearch, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if name := req.GetString("name", ""); name != "" {
			params.Set("name", name)
		}
		if level := req.GetString("level", ""); level != "" {
			params.Set("level", level)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		out, err := util.HTTPGet("/api/security/vul-search", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	vulFindingDetail := mcp.NewTool("vul_finding_detail",
		mcp.WithDescription("Get finding detail path by findingId."),
		mcp.WithString("findingId", mcp.Required(), mcp.Description("Finding ID.")),
	)
	s.AddTool(vulFindingDetail, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		findingId, err := req.RequireString("findingId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"findingId": []string{findingId}}
		out, err := util.HTTPGet("/api/security/vul-finding", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}

func registerScaLeakGadget(s *server.MCPServer) {
	scaScan := mcp.NewTool("sca_scan",
		mcp.WithDescription("SCA dependency risk scan."),
		mcp.WithString("path", mcp.Description("Scan path (optional).")),
		mcp.WithString("paths", mcp.Description("Multiple paths (optional).")),
		mcp.WithString("log4j", mcp.Description("Enable log4j rules (optional).")),
		mcp.WithString("fastjson", mcp.Description("Enable fastjson rules (optional).")),
		mcp.WithString("shiro", mcp.Description("Enable shiro rules (optional).")),
	)
	s.AddTool(scaScan, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if path := req.GetString("path", ""); path != "" {
			params.Set("path", path)
		}
		if paths := req.GetString("paths", ""); paths != "" {
			params.Set("paths", paths)
		}
		if log4j := req.GetString("log4j", ""); log4j != "" {
			params.Set("log4j", log4j)
		}
		if fastjson := req.GetString("fastjson", ""); fastjson != "" {
			params.Set("fastjson", fastjson)
		}
		if shiro := req.GetString("shiro", ""); shiro != "" {
			params.Set("shiro", shiro)
		}
		out, err := util.HTTPGet("/api/security/sca", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	leakScan := mcp.NewTool("leak_scan",
		mcp.WithDescription("Sensitive info leak scan."),
		mcp.WithString("types", mcp.Description("Leak types (optional).")),
		mcp.WithString("base64", mcp.Description("Enable base64 detection (optional).")),
		mcp.WithString("limit", mcp.Description("Max results (optional).")),
		mcp.WithString("whitelist", mcp.Description("Class/package whitelist (optional).")),
		mcp.WithString("blacklist", mcp.Description("Class/package blacklist (optional).")),
		mcp.WithString("jar", mcp.Description("Jar name filter (optional).")),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(leakScan, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if types := req.GetString("types", ""); types != "" {
			params.Set("types", types)
		}
		if base64 := req.GetString("base64", ""); base64 != "" {
			params.Set("base64", base64)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if whitelist := req.GetString("whitelist", ""); whitelist != "" {
			params.Set("whitelist", whitelist)
		}
		if blacklist := req.GetString("blacklist", ""); blacklist != "" {
			params.Set("blacklist", blacklist)
		}
		if jar := req.GetString("jar", ""); jar != "" {
			params.Set("jar", jar)
		}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		out, err := util.HTTPGet("/api/security/leak", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	gadgetScan := mcp.NewTool("gadget_scan",
		mcp.WithDescription("Gadget dependency scan."),
		mcp.WithString("dir", mcp.Required(), mcp.Description("Dependency directory.")),
		mcp.WithString("native", mcp.Description("Enable native rules (optional).")),
		mcp.WithString("hessian", mcp.Description("Enable hessian rules (optional).")),
		mcp.WithString("fastjson", mcp.Description("Enable fastjson rules (optional).")),
		mcp.WithString("jdbc", mcp.Description("Enable jdbc rules (optional).")),
	)
	s.AddTool(gadgetScan, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		dir, err := req.RequireString("dir")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"dir": []string{dir}}
		if native := req.GetString("native", ""); native != "" {
			params.Set("native", native)
		}
		if hessian := req.GetString("hessian", ""); hessian != "" {
			params.Set("hessian", hessian)
		}
		if fastjson := req.GetString("fastjson", ""); fastjson != "" {
			params.Set("fastjson", fastjson)
		}
		if jdbc := req.GetString("jdbc", ""); jdbc != "" {
			params.Set("jdbc", jdbc)
		}
		out, err := util.HTTPGet("/api/security/gadget", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
