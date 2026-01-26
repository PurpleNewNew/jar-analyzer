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
	"strings"

	"jar-analyzer-mcp/pkg/conf"
	"jar-analyzer-mcp/pkg/log"
	"jar-analyzer-mcp/pkg/util"

	"github.com/mark3labs/mcp-go/mcp"
	"github.com/mark3labs/mcp-go/server"
)

func RegisterDfsTaintTools(s *server.MCPServer) {
	flowStart := mcp.NewTool("flow_start",
		mcp.WithDescription("Start DFS or taint job."),
		mcp.WithString("engine", mcp.Required(), mcp.Description("dfs|taint")),
		mcp.WithString("mode", mcp.Description("DFS mode: sink|source (optional).")),
		mcp.WithString("sinkName", mcp.Description("Built-in sink name list (optional).")),
		mcp.WithString("sinkClass", mcp.Description("Sink class (optional).")),
		mcp.WithString("sinkMethod", mcp.Description("Sink method (optional).")),
		mcp.WithString("sinkDesc", mcp.Description("Sink desc (optional).")),
		mcp.WithString("sourceClass", mcp.Description("Source class (optional).")),
		mcp.WithString("sourceMethod", mcp.Description("Source method (optional).")),
		mcp.WithString("sourceDesc", mcp.Description("Source desc (optional).")),
		mcp.WithString("searchAllSources", mcp.Description("Search all sources (optional).")),
		mcp.WithString("onlyFromWeb", mcp.Description("Only from web entrypoints (optional).")),
		mcp.WithString("depth", mcp.Description("DFS depth (optional).")),
		mcp.WithString("maxLimit", mcp.Description("DFS max edges (optional).")),
		mcp.WithString("maxPaths", mcp.Description("DFS max paths (optional).")),
		mcp.WithString("timeoutMs", mcp.Description("Timeout ms (optional).")),
		mcp.WithString("blacklist", mcp.Description("Blacklist classes/packages (optional).")),
		mcp.WithString("dfsJobId", mcp.Description("DFS job id (required for taint).")),
	)
	s.AddTool(flowStart, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		engine := strings.ToLower(strings.TrimSpace(req.GetString("engine", "")))
		if engine == "" {
			return mcp.NewToolResultError("engine is required"), nil
		}
		params := url.Values{}
		if engine == "dfs" {
			addIf(params, "mode", req.GetString("mode", ""))
			addIf(params, "sinkName", req.GetString("sinkName", ""))
			addIf(params, "sinkClass", req.GetString("sinkClass", ""))
			addIf(params, "sinkMethod", req.GetString("sinkMethod", ""))
			addIf(params, "sinkDesc", req.GetString("sinkDesc", ""))
			addIf(params, "sourceClass", req.GetString("sourceClass", ""))
			addIf(params, "sourceMethod", req.GetString("sourceMethod", ""))
			addIf(params, "sourceDesc", req.GetString("sourceDesc", ""))
			addIf(params, "searchAllSources", req.GetString("searchAllSources", ""))
			addIf(params, "onlyFromWeb", req.GetString("onlyFromWeb", ""))
			addIf(params, "depth", req.GetString("depth", ""))
			addIf(params, "maxLimit", req.GetString("maxLimit", ""))
			addIf(params, "maxPaths", req.GetString("maxPaths", ""))
			addIf(params, "timeoutMs", req.GetString("timeoutMs", ""))
			addIf(params, "blacklist", req.GetString("blacklist", ""))
			log.Debugf("call %s", "flow_start")
			out, err := util.HTTPGet("/api/flow/dfs", params)
			if err != nil {
				return mcp.NewToolResultError(err.Error()), nil
			}
			return mcp.NewToolResultText(out), nil
		}
		if engine == "taint" {
			dfsJobId, err := req.RequireString("dfsJobId")
			if err != nil {
				return mcp.NewToolResultError(err.Error()), nil
			}
			params.Set("dfsJobId", dfsJobId)
			addIf(params, "timeoutMs", req.GetString("timeoutMs", ""))
			addIf(params, "maxPaths", req.GetString("maxPaths", ""))
			log.Debugf("call %s", "flow_start")
			out, err := util.HTTPGet("/api/flow/taint", params)
			if err != nil {
				return mcp.NewToolResultError(err.Error()), nil
			}
			return mcp.NewToolResultText(out), nil
		}
		return mcp.NewToolResultError("engine must be dfs or taint"), nil
	})

	flowJob := mcp.NewTool("flow_job",
		mcp.WithDescription("Query DFS/taint job status or results."),
		mcp.WithString("engine", mcp.Required(), mcp.Description("dfs|taint")),
		mcp.WithString("jobId", mcp.Required(), mcp.Description("Job id.")),
		mcp.WithString("action", mcp.Description("status|results|cancel (optional, default status).")),
		mcp.WithString("offset", mcp.Description("Results offset (optional).")),
		mcp.WithString("limit", mcp.Description("Results limit (optional).")),
		mcp.WithString("compact", mcp.Description("Compact results (optional, dfs only).")),
	)
	s.AddTool(flowJob, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
		if conf.McpAuth {
			if req.Header.Get("Token") == "" {
				return mcp.NewToolResultError("need token error"), nil
			}
			if req.Header.Get("Token") != conf.McpToken {
				return mcp.NewToolResultError("need token error"), nil
			}
		}
		engine := strings.ToLower(strings.TrimSpace(req.GetString("engine", "")))
		if engine == "" {
			return mcp.NewToolResultError("engine is required"), nil
		}
		jobId, err := req.RequireString("jobId")
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		action := strings.ToLower(strings.TrimSpace(req.GetString("action", "")))
		path := "/api/flow/" + engine + "/jobs/" + jobId
		params := url.Values{}
		if action == "results" {
			path = path + "/results"
			addIf(params, "offset", req.GetString("offset", ""))
			addIf(params, "limit", req.GetString("limit", ""))
			addIf(params, "compact", req.GetString("compact", ""))
		} else if action == "cancel" {
			path = path + "/cancel"
		}
		log.Debugf("call %s", "flow_job")
		out, err := util.HTTPGet(path, params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}

func addIf(params url.Values, key, value string) {
	if strings.TrimSpace(value) == "" {
		return
	}
	params.Set(key, value)
}
