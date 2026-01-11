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

func RegisterDfsTools(s *server.MCPServer) {
	getSinksTool := mcp.NewTool("get_sinks",
		mcp.WithDescription("获取内置 SINK 规则列表"),
	)
	s.AddTool(getSinksTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		out, err := util.HTTPGet("/api/get_sinks", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getDfsTool := mcp.NewTool("get_dfs_chains",
		mcp.WithDescription("DFS 调用链分析（异步任务创建，返回 jobId）"),
		mcp.WithString("mode", mcp.Description("sink 或 source（可选，默认 sink）")),
		mcp.WithString("fromSink", mcp.Description("是否从 SINK 开始（可选）")),
		mcp.WithString("fromSource", mcp.Description("是否从 SOURCE 开始（可选）")),
		mcp.WithString("sinkName", mcp.Description("内置 SINK 名称（可选，优先使用）")),
		mcp.WithString("sinkClass", mcp.Description("SINK 类名（可选）")),
		mcp.WithString("sinkMethod", mcp.Description("SINK 方法名（可选）")),
		mcp.WithString("sinkDesc", mcp.Description("SINK 方法描述（可选）")),
		mcp.WithString("sourceClass", mcp.Description("SOURCE 类名（可选）")),
		mcp.WithString("sourceMethod", mcp.Description("SOURCE 方法名（可选）")),
		mcp.WithString("sourceDesc", mcp.Description("SOURCE 方法描述（可选）")),
		mcp.WithString("searchAllSources", mcp.Description("从 SINK 反向查找全部 SOURCE（可选）")),
		mcp.WithString("onlyFromWeb", mcp.Description("只从 Spring/Servlet 入口找 SOURCE（可选）")),
		mcp.WithString("depth", mcp.Description("最大深度（可选，默认 10）")),
		mcp.WithString("maxLimit", mcp.Description("最大链路数量（可选）")),
		mcp.WithString("maxPaths", mcp.Description("最大路径数量（可选）")),
		mcp.WithString("maxNodes", mcp.Description("最大节点数量（可选）")),
		mcp.WithString("maxEdges", mcp.Description("最大边数量（可选）")),
		mcp.WithString("timeoutMs", mcp.Description("服务端超时毫秒（可选）")),
		mcp.WithString("blacklist", mcp.Description("黑名单类名（逗号/换行分隔，可选）")),
	)
	s.AddTool(getDfsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		params := url.Values{}
		if mode := req.GetString("mode", ""); mode != "" {
			params.Set("mode", mode)
		}
		if fromSink := req.GetString("fromSink", ""); fromSink != "" {
			params.Set("fromSink", fromSink)
		}
		if fromSource := req.GetString("fromSource", ""); fromSource != "" {
			params.Set("fromSource", fromSource)
		}
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
		if sourceClass := req.GetString("sourceClass", ""); sourceClass != "" {
			params.Set("sourceClass", sourceClass)
		}
		if sourceMethod := req.GetString("sourceMethod", ""); sourceMethod != "" {
			params.Set("sourceMethod", sourceMethod)
		}
		if sourceDesc := req.GetString("sourceDesc", ""); sourceDesc != "" {
			params.Set("sourceDesc", sourceDesc)
		}
		if searchAllSources := req.GetString("searchAllSources", ""); searchAllSources != "" {
			params.Set("searchAllSources", searchAllSources)
		}
		if onlyFromWeb := req.GetString("onlyFromWeb", ""); onlyFromWeb != "" {
			params.Set("onlyFromWeb", onlyFromWeb)
		}
		if depth := req.GetString("depth", ""); depth != "" {
			params.Set("depth", depth)
		}
		if maxLimit := req.GetString("maxLimit", ""); maxLimit != "" {
			params.Set("maxLimit", maxLimit)
		}
		if maxPaths := req.GetString("maxPaths", ""); maxPaths != "" {
			params.Set("maxPaths", maxPaths)
		}
		if maxNodes := req.GetString("maxNodes", ""); maxNodes != "" {
			params.Set("maxNodes", maxNodes)
		}
		if maxEdges := req.GetString("maxEdges", ""); maxEdges != "" {
			params.Set("maxEdges", maxEdges)
		}
		if timeoutMs := req.GetString("timeoutMs", ""); timeoutMs != "" {
			params.Set("timeoutMs", timeoutMs)
		}
		if blacklist := req.GetString("blacklist", ""); blacklist != "" {
			params.Set("blacklist", blacklist)
		}
		out, err := util.HTTPGet("/api/dfs", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getDfsJobTool := mcp.NewTool("get_dfs_job",
		mcp.WithDescription("查询 DFS 异步任务状态"),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("任务 ID")),
	)
	s.AddTool(getDfsJobTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		out, err := util.HTTPGet("/api/dfs/jobs/"+jobId, nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getDfsResultsTool := mcp.NewTool("get_dfs_results",
		mcp.WithDescription("分页获取 DFS 异步任务结果"),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("任务 ID")),
		mcp.WithString("offset", mcp.Description("分页偏移（可选）")),
		mcp.WithString("limit", mcp.Description("分页大小（可选）")),
		mcp.WithString("compact", mcp.Description("是否返回精简结果（可选）")),
	)
	s.AddTool(getDfsResultsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		if compact := req.GetString("compact", ""); compact != "" {
			params.Set("compact", compact)
		}
		out, err := util.HTTPGet("/api/dfs/jobs/"+jobId+"/results", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	cancelDfsJobTool := mcp.NewTool("cancel_dfs_job",
		mcp.WithDescription("取消 DFS 异步任务"),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("任务 ID")),
	)
	s.AddTool(cancelDfsJobTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		out, err := util.HTTPGet("/api/dfs/jobs/"+jobId+"/cancel", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	taintJobTool := mcp.NewTool("taint_job",
		mcp.WithDescription("基于 DFS job 的异步污点分析"),
		mcp.WithString("dfsJobId", mcp.Required(), mcp.Description("DFS 任务 ID")),
		mcp.WithString("timeoutMs", mcp.Description("服务端超时毫秒（可选）")),
		mcp.WithString("maxPaths", mcp.Description("最大路径数量（可选）")),
	)
	s.AddTool(taintJobTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		dfsJobId, err := req.RequireString("dfsJobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{"dfsJobId": []string{dfsJobId}}
		if timeoutMs := req.GetString("timeoutMs", ""); timeoutMs != "" {
			params.Set("timeoutMs", timeoutMs)
		}
		if maxPaths := req.GetString("maxPaths", ""); maxPaths != "" {
			params.Set("maxPaths", maxPaths)
		}
		out, err := util.HTTPGet("/api/taint/jobs", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getTaintJobTool := mcp.NewTool("get_taint_job",
		mcp.WithDescription("查询污点异步任务状态"),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("任务 ID")),
	)
	s.AddTool(getTaintJobTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		out, err := util.HTTPGet("/api/taint/jobs/"+jobId, nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	getTaintResultsTool := mcp.NewTool("get_taint_results",
		mcp.WithDescription("分页获取污点异步任务结果"),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("任务 ID")),
		mcp.WithString("offset", mcp.Description("分页偏移（可选）")),
		mcp.WithString("limit", mcp.Description("分页大小（可选）")),
	)
	s.AddTool(getTaintResultsTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		params := url.Values{}
		if offset := req.GetString("offset", ""); offset != "" {
			params.Set("offset", offset)
		}
		if limit := req.GetString("limit", ""); limit != "" {
			params.Set("limit", limit)
		}
		out, err := util.HTTPGet("/api/taint/jobs/"+jobId+"/results", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	cancelTaintJobTool := mcp.NewTool("cancel_taint_job",
		mcp.WithDescription("取消污点异步任务"),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("任务 ID")),
	)
	s.AddTool(cancelTaintJobTool, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		out, err := util.HTTPGet("/api/taint/jobs/"+jobId+"/cancel", nil)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	log.Debug("register dfs tools")
}
