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

func RegisterSecurityTools(s *server.MCPServer) {
	scaScanTool := mcp.NewTool("sca_scan",
		mcp.WithDescription("SCA 依赖风险扫描"),
		mcp.WithString("path", mcp.Description("扫描路径（文件或目录，可选）")),
		mcp.WithString("paths", mcp.Description("多个路径，逗号/换行分隔（可选）")),
		mcp.WithString("log4j", mcp.Description("是否启用 Log4j2 规则（可选，默认 true）")),
		mcp.WithString("fastjson", mcp.Description("是否启用 Fastjson 规则（可选，默认 true）")),
		mcp.WithString("shiro", mcp.Description("是否启用 Shiro 规则（可选，默认 true）")),
	)
	s.AddTool(scaScanTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if paths := req.GetString("paths", ""); paths != "" {
			params.Set("paths", paths)
		}
		if log4j := req.GetString("log4j", ""); log4j != "" {
			params.Set("log4j", log4j)
		}
		if fastjson := req.GetString("fastjson", ""); fastjson != "" {
			params.Set("fastjson", fastjson)
		}
		if shiro := req.GetString("shiro", ""); shiro != "" {
			params.Set("shiro", shiro)
		}
		out, err := util.HTTPGet("/api/sca", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	leakScanTool := mcp.NewTool("leak_scan",
		mcp.WithDescription("敏感信息泄露扫描"),
		mcp.WithString("types", mcp.Description("规则类型列表，逗号/换行分隔（可选，默认全部）")),
		mcp.WithString("base64", mcp.Description("是否开启 Base64 检测（可选）")),
		mcp.WithString("limit", mcp.Description("最大返回数量（可选）")),
	)
	s.AddTool(leakScanTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if types := req.GetString("types", ""); types != "" {
			params.Set("types", types)
		}
		if base64 := req.GetString("base64", ""); base64 != "" {
			params.Set("base64", base64)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		out, err := util.HTTPGet("/api/leak", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	gadgetScanTool := mcp.NewTool("gadget_scan",
		mcp.WithDescription("Gadget 依赖扫描（基于 JAR 名称匹配）"),
		mcp.WithString("dir", mcp.Required(), mcp.Description("依赖目录（必须）")),
		mcp.WithString("native", mcp.Description("启用 Native 规则（可选，默认 true）")),
		mcp.WithString("hessian", mcp.Description("启用 Hessian 规则（可选，默认 true）")),
		mcp.WithString("fastjson", mcp.Description("启用 Fastjson 规则（可选，默认 true）")),
		mcp.WithString("jdbc", mcp.Description("启用 JDBC 规则（可选，默认 true）")),
	)
	s.AddTool(gadgetScanTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		dir, err := req.RequireString("dir")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"dir": []string{dir}}
		if native := req.GetString("native", ""); native != "" {
			params.Set("native", native)
		}
		if hessian := req.GetString("hessian", ""); hessian != "" {
			params.Set("hessian", hessian)
		}
		if fastjson := req.GetString("fastjson", ""); fastjson != "" {
			params.Set("fastjson", fastjson)
		}
		if jdbc := req.GetString("jdbc", ""); jdbc != "" {
			params.Set("jdbc", jdbc)
		}
		out, err := util.HTTPGet("/api/gadget", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	log.Debug("register sca/leak/gadget tools")
}

