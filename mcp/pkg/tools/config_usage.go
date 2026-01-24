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
	getConfigUsageTool := mcp.NewTool("get_config_usage",
		mcp.WithDescription("连接配置/泄露关键字与代码使用点（返回结构化证据链）"),
		mcp.WithString("keys", mcp.Description("关键字列表，JSON 数组或逗号分隔（可选）")),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("maxKeys", mcp.Description("最多 key 数（可选）")),
		mcp.WithString("maxPerKey", mcp.Description("每个 key 返回方法数量上限（可选）")),
		mcp.WithString("maxResources", mcp.Description("每个 key 返回资源数量上限（可选）")),
		mcp.WithString("maxBytes", mcp.Description("读取资源最大字节数（可选）")),
		mcp.WithString("maxDepth", mcp.Description("调用链回溯深度（可选）")),
		mcp.WithString("mappingLimit", mcp.Description("Spring mapping 最大数量（可选）")),
		mcp.WithString("mask", mcp.Description("是否掩码 value（1/0，可选）")),
	)
	s.AddTool(getConfigUsageTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if maxResources := req.GetString("maxResources", ""); maxResources != "" {
			params.Set("maxResources", maxResources)
		}
		if maxBytes := req.GetString("maxBytes", ""); maxBytes != "" {
			params.Set("maxBytes", maxBytes)
		}
		if maxDepth := req.GetString("maxDepth", ""); maxDepth != "" {
			params.Set("maxDepth", maxDepth)
		}
		if mappingLimit := req.GetString("mappingLimit", ""); mappingLimit != "" {
			params.Set("mappingLimit", mappingLimit)
		}
		if mask := req.GetString("mask", ""); mask != "" {
			params.Set("mask", mask)
		}
		out, err := util.HTTPGet("/api/get_config_usage", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	log.Debug("register config usage tools")
}
