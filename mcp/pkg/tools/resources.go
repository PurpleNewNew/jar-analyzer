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

func RegisterResourceTools(s *server.MCPServer) {
	resList := mcp.NewTool("resources_list",
		mcp.WithDescription("List resources."),
		mcp.WithString("path", mcp.Description("Path prefix (optional).")),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
	)
	s.AddTool(resList, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		log.Debugf("call %s", "resources_list")
		out, err := util.HTTPGet("/api/resources/list", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	resGet := mcp.NewTool("resources_get",
		mcp.WithDescription("Get resource content by id or jarId+path."),
		mcp.WithString("id", mcp.Description("Resource id (optional).")),
		mcp.WithString("jarId", mcp.Description("Jar ID (optional, required for path lookup).")),
		mcp.WithString("path", mcp.Description("Resource path (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
		mcp.WithString("base64", mcp.Description("Force base64 (optional).")),
	)
	s.AddTool(resGet, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if id := req.GetString("id", ""); id != "" {
			params.Set("id", id)
		}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if path := req.GetString("path", ""); path != "" {
			params.Set("path", path)
		}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if base64 := req.GetString("base64", ""); base64 != "" {
			params.Set("base64", base64)
		}
		log.Debugf("call %s", "resources_get")
		out, err := util.HTTPGet("/api/resources/get", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	resSearch := mcp.NewTool("resources_search",
		mcp.WithDescription("Search text resources by keywords."),
		mcp.WithString("query", mcp.Required(), mcp.Description("Search keywords (comma/newline separated).")),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
		mcp.WithString("maxBytes", mcp.Description("Max bytes to read per file (optional).")),
		mcp.WithString("mode", mcp.Description("Match mode: and|or (optional).")),
		mcp.WithString("case", mcp.Description("Case sensitive (optional).")),
	)
	s.AddTool(resSearch, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		query, err := req.RequireString("query")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"query": []string{query}}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if maxBytes := req.GetString("maxBytes", ""); maxBytes != "" {
			params.Set("maxBytes", maxBytes)
		}
		if mode := req.GetString("mode", ""); mode != "" {
			params.Set("mode", mode)
		}
		if caseSensitive := req.GetString("case", ""); caseSensitive != "" {
			params.Set("case", caseSensitive)
		}
		log.Debugf("call %s", "resources_search")
		out, err := util.HTTPGet("/api/resources/search", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
