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
	getSemanticHintsTool := mcp.NewTool("get_semantic_hints",
		mcp.WithDescription("获取语义提示（认证/授权/校验/配置等）"),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("limit", mcp.Description("每个类别返回上限（可选）")),
		mcp.WithString("strLimit", mcp.Description("字符串关键字搜索上限（可选）")),
	)
	s.AddTool(getSemanticHintsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		out, err := util.HTTPGet("/api/get_semantic_hints", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	log.Debug("register semantic tools")
}
