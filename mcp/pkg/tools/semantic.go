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

func RegisterSemanticTools(s *server.MCPServer) {
	semantic := mcp.NewTool("semantic_hints",
		mcp.WithDescription("Get semantic hints (authn/authz/validation/config boundaries)."),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("limit", mcp.Description("Limit per category (optional).")),
		mcp.WithString("strLimit", mcp.Description("String search limit (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(semantic, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if strLimit := req.GetString("strLimit", ""); strLimit != "" {
			params.Set("strLimit", strLimit)
		}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "semantic_hints")
		out, err := util.HTTPGet("/api/semantic/hints", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
