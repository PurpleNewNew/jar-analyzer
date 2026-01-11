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
	"net/url"

	"jar-analyzer-mcp/pkg/util"

	"github.com/mark3labs/mcp-go/mcp"
	"github.com/mark3labs/mcp-go/server"
)

func RegisterMethodClassTools(s *server.MCPServer) {
	getMethodsByClassTool := mcp.NewTool("get_methods_by_class",
		mcp.WithDescription("查询指定类中的所有方法信息"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名（点或斜杠分隔均可）")),
	)
	s.AddTool(getMethodsByClassTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		log.Debugf("call %s, class: %s", "get_methods_by_class", className)
		params := url.Values{"class": []string{className}}
		out, err := util.HTTPGet("/api/get_methods_by_class", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getMethodsByStrTool := mcp.NewTool("get_methods_by_str",
		mcp.WithDescription("搜索包含指定字符串(String类型的变量、注解) 的方法（模糊）"),
		mcp.WithString("str", mcp.Required(), mcp.Description("搜索关键 字")),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("class", mcp.Description("类名前缀（可选）")),
		mcp.WithString("package", mcp.Description("包名前缀（可选）")),
		mcp.WithString("limit", mcp.Description("返回数量限制（可选）")),
		mcp.WithString("mode", mcp.Description("搜索模式: auto|contains|prefix|equal|fts（可选）")),
	)
	s.AddTool(getMethodsByStrTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		q, err := req.RequireString("str")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		log.Debugf("call %s, str: %s", "get_methods_by_str", q)
		params := url.Values{"str": []string{q}}
		if jarId := req.GetString("jarId", ""); jarId != "" {
			params.Set("jarId", jarId)
		}
		if className := req.GetString("class", ""); className != "" {
			params.Set("class", className)
		}
		if pkg := req.GetString("package", ""); pkg != "" {
			params.Set("package", pkg)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if mode := req.GetString("mode", ""); mode != "" {
			params.Set("mode", mode)
		}
		out, err := util.HTTPGet("/api/get_methods_by_str", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getMethodsByStrBatchTool := mcp.NewTool("get_methods_by_str_batch",
		mcp.WithDescription("批量搜索包含指定字符串的方法"),
		mcp.WithString("items", mcp.Required(), mcp.Description("JSON 数组: [{str,class,package,pkg,jarId,limit,mode}]")),
		mcp.WithString("limit", mcp.Description("默认返回数量限制（可选）")),
	)
	s.AddTool(getMethodsByStrBatchTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		items, err := req.RequireString("items")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"items": []string{items}}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		out, err := util.HTTPGet("/api/get_methods_by_str_batch", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getClassByClassTool := mcp.NewTool("get_class_by_class",
		mcp.WithDescription("查询类的基本信息"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名（点或斜杠分隔均可）")),
	)
	s.AddTool(getClassByClassTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		log.Debugf("call %s, class: %s", "get_class_by_class", className)
		params := url.Values{"class": []string{className}}
		out, err := util.HTTPGet("/api/get_class_by_class", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
