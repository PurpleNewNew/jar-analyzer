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

func RegisterCallGraphTools(s *server.MCPServer) {
	callArgs := func(req mcp.CallToolRequest) (string, string, string, *mcp.CallToolResult) {
		clazz, err := req.RequireString("class")
		if err != nil {
			return "", "", "", mcp.NewToolResultError(err.Error())
		}
		method, err := req.RequireString("method")
		if err != nil {
			return "", "", "", mcp.NewToolResultError(err.Error())
		}
		desc := req.GetString("desc", "")
		return clazz, method, desc, nil
	}

	getCallersTool := mcp.NewTool("get_callers",
		mcp.WithDescription("查询方法的所有调用者"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getCallersTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_callers", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_callers", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getCallersBySinkTool := mcp.NewTool("get_callers_by_sink",
		mcp.WithDescription("根据内置或自定义 SINK 查询调用点"),
		mcp.WithString("sinkName", mcp.Description("内置 SINK 名称，逗号分隔（可选）")),
		mcp.WithString("sinkClass", mcp.Description("SINK 类名（可选）")),
		mcp.WithString("sinkMethod", mcp.Description("SINK 方法名（可选）")),
		mcp.WithString("sinkDesc", mcp.Description("SINK 方法描述（可选）")),
		mcp.WithString("items", mcp.Description("批量 SINK JSON 数组（可选）")),
		mcp.WithString("limit", mcp.Description("每个 SINK 最大返回数量（可选）")),
	)
	s.AddTool(getCallersBySinkTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if sinkName := req.GetString("sinkName", ""); sinkName != "" {
			params.Set("sinkName", sinkName)
		}
		if sinkClass := req.GetString("sinkClass", ""); sinkClass != "" {
			params.Set("sinkClass", sinkClass)
		}
		if sinkMethod := req.GetString("sinkMethod", ""); sinkMethod != "" {
			params.Set("sinkMethod", sinkMethod)
		}
		if sinkDesc := req.GetString("sinkDesc", ""); sinkDesc != "" {
			params.Set("sinkDesc", sinkDesc)
		}
		if items := req.GetString("items", ""); items != "" {
			params.Set("items", items)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		out, err := util.HTTPGet("/api/get_callers_by_sink", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getCallersBatchTool := mcp.NewTool("get_callers_batch",
		mcp.WithDescription("批量查询方法的所有调用者"),
		mcp.WithString("items", mcp.Required(), mcp.Description("JSON 数组: [{class,method,desc}]")),
		mcp.WithString("limit", mcp.Description("每条最大返回数量（可选）")),
	)
	s.AddTool(getCallersBatchTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		out, err := util.HTTPGet("/api/get_callers_batch", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getCalleeBatchTool := mcp.NewTool("get_callee_batch",
		mcp.WithDescription("批量查询方法的被调用者"),
		mcp.WithString("items", mcp.Required(), mcp.Description("JSON 数组: [{class,method,desc}]")),
		mcp.WithString("limit", mcp.Description("每条最大返回数量（可选）")),
	)
	s.AddTool(getCalleeBatchTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		out, err := util.HTTPGet("/api/get_callee_batch", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getCallEdgesTool := mcp.NewTool("get_call_edges",
		mcp.WithDescription("查询调用边（带证据/置信度）"),
		mcp.WithString("mode", mcp.Description("callers 或 callees（可选，默认 callers）")),
		mcp.WithString("direction", mcp.Description("兼容参数（可选）")),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
		mcp.WithString("offset", mcp.Description("偏移（可选）")),
		mcp.WithString("limit", mcp.Description("返回数量限制（可选）")),
	)
	s.AddTool(getCallEdgesTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		methodName, err := req.RequireString("method")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		desc := req.GetString("desc", "")
		params := url.Values{"class": []string{className}, "method": []string{methodName}}
		if desc != "" {
			params.Set("desc", desc)
		}
		if mode := req.GetString("mode", ""); mode != "" {
			params.Set("mode", mode)
		}
		if direction := req.GetString("direction", ""); direction != "" {
			params.Set("direction", direction)
		}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		out, err := util.HTTPGet("/api/get_call_edges", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getMethodBatchTool := mcp.NewTool("get_method_batch",
		mcp.WithDescription("批量精确查询方法"),
		mcp.WithString("items", mcp.Required(), mcp.Description("JSON 数组: [{class,method,desc}]")),
		mcp.WithString("limit", mcp.Description("每条最大返回数量（可选）")),
	)
	s.AddTool(getMethodBatchTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		out, err := util.HTTPGet("/api/get_method_batch", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getCallersLikeTool := mcp.NewTool("get_callers_like",
		mcp.WithDescription("模糊查询方法的调用者"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名（模糊）")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getCallersLikeTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_callers_like", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_callers_like", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getCalleeTool := mcp.NewTool("get_callee",
		mcp.WithDescription("查询方法的被调用者"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getCalleeTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_callee", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_callee", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getMethodTool := mcp.NewTool("get_method",
		mcp.WithDescription("精确查询方法"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getMethodTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_method", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_method", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getMethodLikeTool := mcp.NewTool("get_method_like",
		mcp.WithDescription("模糊查询方法"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名（模糊）")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getMethodLikeTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_method_like", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_method_like", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getImplsTool := mcp.NewTool("get_impls",
		mcp.WithDescription("查询接口/抽象方法的实现"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getImplsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_impls", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_impls", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getSuperImplsTool := mcp.NewTool("get_super_impls",
		mcp.WithDescription("查询父类/接口的实现"),
		mcp.WithString("class", mcp.Required(), mcp.Description("类名")),
		mcp.WithString("method", mcp.Required(), mcp.Description("方法名")),
		mcp.WithString("desc", mcp.Description("方法描述（可选）")),
	)
	s.AddTool(getSuperImplsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		clazz, method, desc, errRes := callArgs(req)
		if errRes != nil {
			return errRes, nil
		}
		log.Debugf("call %s, class: %s, method: %s, desc: %s",
			"get_super_impls", clazz, method, desc)
		params := url.Values{"class": []string{clazz}, "method": []string{method}}
		if desc != "" {
			params.Set("desc", desc)
		}
		out, err := util.HTTPGet("/api/get_super_impls", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
