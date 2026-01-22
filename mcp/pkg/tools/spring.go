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

func RegisterSpringTools(s *server.MCPServer) {
	getAllSpringControllersTool := mcp.NewTool("get_all_spring_controllers",
		mcp.WithDescription("列出所有 Spring 控制器类"),
	)
	s.AddTool(getAllSpringControllersTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		log.Debugf("call %s", "get_all_spring_controllers")
		out, err := util.HTTPGet("/api/get_all_spring_controllers", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getAllSpringInterceptorsTool := mcp.NewTool("get_all_spring_interceptors",
		mcp.WithDescription("列出所有 Spring 拦截器类"),
	)
	s.AddTool(getAllSpringInterceptorsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		log.Debugf("call %s", "get_all_spring_interceptors")
		out, err := util.HTTPGet("/api/get_all_spring_interceptors", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getSpringMappingsTool := mcp.NewTool("get_spring_mappings",
		mcp.WithDescription("查询某 Spring 控制器的映射方法"),
		mcp.WithString("class", mcp.Required(), mcp.Description("控制器类名")),
	)
	s.AddTool(getSpringMappingsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		log.Debugf("call %s, class: %s", "get_spring_mappings", className)
		params := url.Values{"class": []string{className}}
		out, err := util.HTTPGet("/api/get_spring_mappings", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getSpringMappingsAllTool := mcp.NewTool("get_spring_mappings_all",
		mcp.WithDescription("列出所有 Spring 映射方法（支持过滤/分页）"),
		mcp.WithString("jarId", mcp.Description("Jar ID（可选）")),
		mcp.WithString("keyword", mcp.Description("关键字过滤（可选）")),
		mcp.WithString("offset", mcp.Description("分页偏移（可选）")),
		mcp.WithString("limit", mcp.Description("分页大小（可选）")),
	)
	s.AddTool(getSpringMappingsAllTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if keyword := req.GetString("keyword", ""); keyword != "" {
			params.Set("keyword", keyword)
		}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		log.Debugf("call %s", "get_spring_mappings_all")
		out, err := util.HTTPGet("/api/get_spring_mappings_all", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
