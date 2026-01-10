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

func RegisterResourceTools(s *server.MCPServer) {
	getResourcesTool := mcp.NewTool("get_resources",
		mcp.WithDescription("资源文件列表（支持 path/jarId 过滤）"),
		mcp.WithString("path", mcp.Description("资源路径关键字（可选）")),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("offset", mcp.Description("分页偏移（可选）")),
		mcp.WithString("limit", mcp.Description("分页大小（可选）")),
	)
	s.AddTool(getResourcesTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		out, err := util.HTTPGet("/api/get_resources", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getResourceTool := mcp.NewTool("get_resource",
		mcp.WithDescription("读取资源文件内容"),
		mcp.WithString("id", mcp.Description("资源ID（可选）")),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("path", mcp.Description("资源路径（可选）")),
		mcp.WithString("offset", mcp.Description("读取偏移（可选）")),
		mcp.WithString("limit", mcp.Description("读取长度（可选）")),
		mcp.WithString("base64", mcp.Description("是否强制 Base64（可选）")),
	)
	s.AddTool(getResourceTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		out, err := util.HTTPGet("/api/get_resource", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	searchResourcesTool := mcp.NewTool("search_resources",
		mcp.WithDescription("搜索资源文件内容"),
		mcp.WithString("query", mcp.Required(), mcp.Description("搜索关键字")),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("limit", mcp.Description("返回数量限制（可选）")),
		mcp.WithString("maxBytes", mcp.Description("单文件最大读取字节（可选）")),
	)
	s.AddTool(searchResourcesTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		query, err := req.RequireString("query")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params.Set("query", query)
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if maxBytes := req.GetString("maxBytes", ""); maxBytes != "" {
			params.Set("maxBytes", maxBytes)
		}
		out, err := util.HTTPGet("/api/search_resources", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	log.Debug("register resource tools")
}
