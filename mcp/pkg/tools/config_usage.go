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

func RegisterConfigUsageTools(s *server.MCPServer) {
	configUsage := mcp.NewTool("config_usage",
		mcp.WithDescription("Link config keys to code usage and entrypoints."),
		mcp.WithString("keys", mcp.Description("Keys list (optional).")),
		mcp.WithString("jarId", mcp.Description("Jar ID filter (optional).")),
		mcp.WithString("maxKeys", mcp.Description("Max keys (optional).")),
		mcp.WithString("maxPerKey", mcp.Description("Max methods per key (optional).")),
		mcp.WithString("maxDepth", mcp.Description("Call graph depth (optional).")),
		mcp.WithString("maxResources", mcp.Description("Max config items (optional).")),
		mcp.WithString("maxBytes", mcp.Description("Max bytes per resource (optional).")),
		mcp.WithString("mappingLimit", mcp.Description("Spring mapping limit (optional).")),
		mcp.WithString("maxEntry", mcp.Description("Max entrypoints per method (optional).")),
		mcp.WithString("mask", mcp.Description("Mask values (optional).")),
		mcp.WithString("includeResources", mcp.Description("Include config resources (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(configUsage, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if keys := req.GetString("keys", ""); keys != "" {
			params.Set("keys", keys)
		}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if maxKeys := req.GetString("maxKeys", ""); maxKeys != "" {
			params.Set("maxKeys", maxKeys)
		}
		if maxPerKey := req.GetString("maxPerKey", ""); maxPerKey != "" {
			params.Set("maxPerKey", maxPerKey)
		}
		if maxDepth := req.GetString("maxDepth", ""); maxDepth != "" {
			params.Set("maxDepth", maxDepth)
		}
		if maxResources := req.GetString("maxResources", ""); maxResources != "" {
			params.Set("maxResources", maxResources)
		}
		if maxBytes := req.GetString("maxBytes", ""); maxBytes != "" {
			params.Set("maxBytes", maxBytes)
		}
		if mappingLimit := req.GetString("mappingLimit", ""); mappingLimit != "" {
			params.Set("mappingLimit", mappingLimit)
		}
		if maxEntry := req.GetString("maxEntry", ""); maxEntry != "" {
			params.Set("maxEntry", maxEntry)
		}
		if mask := req.GetString("mask", ""); mask != "" {
			params.Set("mask", mask)
		}
		if includeResources := req.GetString("includeResources", ""); includeResources != "" {
			params.Set("includeResources", includeResources)
		}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "config_usage")
		out, err := util.HTTPGet("/api/config/usage", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
