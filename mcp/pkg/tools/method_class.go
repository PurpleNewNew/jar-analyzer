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

func RegisterMethodClassTools(s *server.MCPServer) {
	methodsSearch := mcp.NewTool("methods_search",
		mcp.WithDescription("Search methods by signature, string, or annotation."),
		mcp.WithString("class", mcp.Description("Class name (optional).")),
		mcp.WithString("method", mcp.Description("Method name (optional).")),
		mcp.WithString("desc", mcp.Description("Method descriptor (optional).")),
		mcp.WithString("match", mcp.Description("Signature match: exact|like (optional).")),
		mcp.WithString("str", mcp.Description("String query (optional).")),
		mcp.WithString("strMode", mcp.Description("String match: auto|contains|prefix|equal|fts (optional).")),
		mcp.WithString("classLike", mcp.Description("Class prefix filter for string search (optional).")),
		mcp.WithString("anno", mcp.Description("Annotation names list (optional).")),
		mcp.WithString("annoMatch", mcp.Description("Annotation match: contains|equal (optional).")),
		mcp.WithString("annoScope", mcp.Description("Annotation scope: any|class|method (optional).")),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(methodsSearch, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if method := req.GetString("method", ""); method != "" {
			params.Set("method", method)
		}
		if desc := req.GetString("desc", ""); desc != "" {
			params.Set("desc", desc)
		}
		if match := req.GetString("match", ""); match != "" {
			params.Set("match", match)
		}
		if str := req.GetString("str", ""); str != "" {
			params.Set("str", str)
		}
		if strMode := req.GetString("strMode", ""); strMode != "" {
			params.Set("strMode", strMode)
		}
		if classLike := req.GetString("classLike", ""); classLike != "" {
			params.Set("classLike", classLike)
		}
		if anno := req.GetString("anno", ""); anno != "" {
			params.Set("anno", anno)
		}
		if annoMatch := req.GetString("annoMatch", ""); annoMatch != "" {
			params.Set("annoMatch", annoMatch)
		}
		if annoScope := req.GetString("annoScope", ""); annoScope != "" {
			params.Set("annoScope", annoScope)
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
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "methods_search")
		out, err := util.HTTPGet("/api/methods/search", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	methodsImpls := mcp.NewTool("methods_impls",
		mcp.WithDescription("Resolve method implementations or super implementations."),
		mcp.WithString("class", mcp.Required(), mcp.Description("Class name (dot or slash).")),
		mcp.WithString("method", mcp.Required(), mcp.Description("Method name.")),
		mcp.WithString("desc", mcp.Description("Method descriptor (optional).")),
		mcp.WithString("direction", mcp.Description("impls|super (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(methodsImpls, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if desc := req.GetString("desc", ""); desc != "" {
			params.Set("desc", desc)
		}
		if direction := req.GetString("direction", ""); direction != "" {
			params.Set("direction", direction)
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
		log.Debugf("call %s", "methods_impls")
		out, err := util.HTTPGet("/api/methods/impls", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
