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

func RegisterSpringTools(s *server.MCPServer) {
	entrypoints := mcp.NewTool("entrypoints_list",
		mcp.WithDescription("List entrypoint classes by type."),
		mcp.WithString("type", mcp.Required(), mcp.Description("Types: spring_controller, spring_interceptor, servlet, filter, listener, all.")),
		mcp.WithString("limit", mcp.Description("Limit per type (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(entrypoints, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		types, err := req.RequireString("type")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"type": []string{types}}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "entrypoints_list")
		out, err := util.HTTPGet("/api/entrypoints", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	mappings := mcp.NewTool("spring_mappings",
		mcp.WithDescription("List Spring mappings (class-specific or global search)."),
		mcp.WithString("class", mcp.Description("Class name (optional).")),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("keyword", mcp.Description("Keyword filter (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(mappings, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if className := req.GetString("class", ""); className != "" {
			params.Set("class", className)
		}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if keyword := req.GetString("keyword", ""); keyword != "" {
			params.Set("keyword", keyword)
		}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "spring_mappings")
		out, err := util.HTTPGet("/api/entrypoints/mappings", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
