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
	getJarsListTool := mcp.NewTool("get_jars_list",
		mcp.WithDescription("查询所有输入的 JAR 文件"),
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
		out, err := util.HTTPGet("/api/get_jars_list", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}

func RegisterJarResolveTools(s *server.MCPServer) {
	getJarByClassTool := mcp.NewTool("get_jar_by_class",
		mcp.WithDescription("根据类名查询归属 JAR"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名（点或斜杠分隔均可）")),
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
		log.Debugf("call %s, class: %s", "get_jar_by_class", className)
		params := url.Values{"class": []string{className}}
		out, err := util.HTTPGet("/api/get_jar_by_class", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getAbsPathTool := mcp.NewTool("get_abs_path",
		mcp.WithDescription("获取 CLASS 文件的本地绝对路径"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名（点或斜杠分隔均可）")),
	)
	s.AddTool(getAbsPathTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		log.Debugf("call %s, class: %s", "get_abs_path", className)
		params := url.Values{"class": []string{className}}
		out, err := util.HTTPGet("/api/get_abs_path", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}

func RegisterJarTools(s *server.MCPServer) {
	RegisterJarMetaTools(s)
	RegisterJarResolveTools(s)
	log.Debug("register jar tools")
}
