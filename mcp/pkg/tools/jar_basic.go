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

func RegisterJarMetaTools(s *server.MCPServer) {
	getJarsListTool := mcp.NewTool("jar_list",
		mcp.WithDescription("List jar metadata (jar_id/jar_name/jar_fingerprint)."),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
	)
	s.AddTool(getJarsListTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		out, err := util.HTTPGet("/api/meta/jars", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}

func RegisterJarResolveTools(s *server.MCPServer) {
	getJarByClassTool := mcp.NewTool("jar_resolve",
		mcp.WithDescription("Resolve jar metadata by class name."),
		mcp.WithString("class", mcp.Required(), mcp.Description("Class name (dot or slash).")),
	)
	s.AddTool(getJarByClassTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		log.Debugf("call %s, class: %s", "jar_resolve", className)
		params := url.Values{"class": []string{className}}
		out, err := util.HTTPGet("/api/meta/jars/resolve", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}

func RegisterJarTools(s *server.MCPServer) {
	RegisterJarMetaTools(s)
	RegisterJarResolveTools(s)
	registerClassInfoTool(s)
	log.Debug("register jar tools")
}

func registerClassInfoTool(s *server.MCPServer) {
	tool := mcp.NewTool("class_info",
		mcp.WithDescription("Get class info (super class, interface, jar)."),
		mcp.WithString("class", mcp.Required(), mcp.Description("Class name (dot or slash).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(tool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		params := url.Values{"class": []string{className}}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		out, err := util.HTTPGet("/api/class/info", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
