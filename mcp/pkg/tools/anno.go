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
	"jar-analyzer-mcp/pkg/conf"
	"jar-analyzer-mcp/pkg/log"
	"jar-analyzer-mcp/pkg/util"
	"net/url"

	"github.com/mark3labs/mcp-go/mcp"
	"github.com/mark3labs/mcp-go/server"
)

func RegisterAnnoTools(s *server.MCPServer) {
	getMethodsByAnnoTool := mcp.NewTool("get_methods_by_anno",
		mcp.WithDescription("根据注解名搜索方法（支持 contains/equal, class/method/any）"),
		mcp.WithString("anno", mcp.Description("注解名（逗号/换行分隔，可选）")),
		mcp.WithString("items", mcp.Description("JSON 数组注解名（可选）")),
		mcp.WithString("match", mcp.Description("匹配模式: contains|equal（可选，默认 contains）")),
		mcp.WithString("scope", mcp.Description("作用域: any|class|method（可选）")),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("offset", mcp.Description("偏移（可选）")),
		mcp.WithString("limit", mcp.Description("返回数量限制（可选）")),
	)
	s.AddTool(getMethodsByAnnoTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if anno := req.GetString("anno", ""); anno != "" {
			params.Set("anno", anno)
		}
		if items := req.GetString("items", ""); items != "" {
			params.Set("items", items)
		}
		if match := req.GetString("match", ""); match != "" {
			params.Set("match", match)
		}
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
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
		log.Debugf("call %s, anno: %s", "get_methods_by_anno", params.Encode())
		out, err := util.HTTPGet("/api/get_methods_by_anno", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
