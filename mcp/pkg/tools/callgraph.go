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

func RegisterCallGraphTools(s *server.MCPServer) {
	callEdges := mcp.NewTool("callgraph_edges",
		mcp.WithDescription("Query call graph edges with evidence/confidence."),
		mcp.WithString("class", mcp.Required(), mcp.Description("Class name.")),
		mcp.WithString("method", mcp.Required(), mcp.Description("Method name.")),
		mcp.WithString("desc", mcp.Description("Method descriptor (optional).")),
		mcp.WithString("direction", mcp.Description("callers|callees (optional).")),
		mcp.WithString("offset", mcp.Description("Offset (optional).")),
		mcp.WithString("limit", mcp.Description("Limit (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(callEdges, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		params := url.Values{"class": []string{className}, "method": []string{methodName}}
		if desc := req.GetString("desc", ""); desc != "" {
			params.Set("desc", desc)
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
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "callgraph_edges")
		out, err := util.HTTPGet("/api/callgraph/edges", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})

	callBySink := mcp.NewTool("callgraph_by_sink",
		mcp.WithDescription("Find callers by sink definition or sink name."),
		mcp.WithString("sinkName", mcp.Description("Built-in sink name list (comma separated).")),
		mcp.WithString("sinkClass", mcp.Description("Sink class (optional).")),
		mcp.WithString("sinkMethod", mcp.Description("Sink method (optional).")),
		mcp.WithString("sinkDesc", mcp.Description("Sink desc (optional).")),
		mcp.WithString("items", mcp.Description("Batch JSON array of sinks (optional).")),
		mcp.WithString("limit", mcp.Description("Per-sink limit (optional).")),
		mcp.WithString("scope", mcp.Description("Scope filter: app|all (optional).")),
	)
	s.AddTool(callBySink, func(ctx context.Context, req mcp.CallToolRequest) (*mcp.CallToolResult, error) {
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
		if scope := req.GetString("scope", ""); scope != "" {
			params.Set("scope", scope)
		}
		log.Debugf("call %s", "callgraph_by_sink")
		out, err := util.HTTPGet("/api/callgraph/by-sink", params)
		if err != nil {
			return mcp.NewToolResultError(err.Error()), nil
		}
		return mcp.NewToolResultText(out), nil
	})
}
