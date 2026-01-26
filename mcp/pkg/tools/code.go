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

func RegisterCodeTools(s *server.MCPServer) {
	codeGet := mcp.NewTool("code_get",
		mcp.WithDescription("Get decompiled method code (CFR or Fernflower)."),
		mcp.WithString("engine", mcp.Description("cfr|fernflower (optional, default cfr).")),
		mcp.WithString("class", mcp.Required(), mcp.Description("Class name.")),
		mcp.WithString("method", mcp.Required(), mcp.Description("Method name.")),
		mcp.WithString("desc", mcp.Description("Method descriptor (optional).")),
		mcp.WithString("full", mcp.Description("Include full class code (optional).")),
	)
	s.AddTool(codeGet, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		className, err := req.RequireString("class")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		methodName, err := req.RequireString("method")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"class": []string{className}, "method": []string{methodName}}
		if engine := req.GetString("engine", ""); engine != "" {
			params.Set("engine", engine)
		}
		if desc := req.GetString("desc", ""); desc != "" {
			params.Set("desc", desc)
		}
		if full := req.GetString("full", ""); full != "" {
			params.Set("full", full)
		}
		log.Debugf("call %s", "code_get")
		out, err := util.HTTPGet("/api/code", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
