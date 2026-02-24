#!/usr/bin/env python3
"""
MST Maneuver Graph Visualization using Graphviz

This creates a hierarchical visualization of the MST graph that better handles
large trees. It uses DOT language to create the graph and renders it with graphviz.

Features:
- Proper tree layout (top to bottom)
- Each node shows 4 compartments as an HTML-like table
- Best path highlighted in red
- Edge labels with transition probabilities
- Color-coded confidence levels

Usage:
    python mst_graph_visualizer_graphviz.py <input_json_file> [output_file]
    
Dependencies:
    pip install graphviz
    
Also requires graphviz to be installed on the system:
    sudo apt-get install graphviz  # Ubuntu/Debian
    brew install graphviz          # macOS
"""

import json
import sys
from graphviz import Digraph

# Maneuver type colors (HTML hex format)
TYPE_COLORS = {
    'TACK': '#4CAF50',       # Green
    'JIBE': '#2196F3',       # Blue
    'HEAD_UP': '#FF9800',    # Orange
    'BEAR_AWAY': '#9C27B0',  # Purple
}

TYPE_ORDER = ['TACK', 'JIBE', 'HEAD_UP', 'BEAR_AWAY']
TYPE_ABBREV = {'TACK': 'T', 'JIBE': 'J', 'HEAD_UP': 'H', 'BEAR_AWAY': 'B'}


def confidence_to_intensity(confidence):
    """Convert confidence [0,1] to color intensity for background."""
    # Higher confidence = more saturated color
    base_intensity = 40  # Minimum intensity (very light)
    max_intensity = 200   # Maximum intensity
    intensity = int(base_intensity + confidence * (max_intensity - base_intensity))
    return intensity


def format_wind(comp):
    """Format wind direction string."""
    wind_est = comp.get('windEstimate', comp.get('windRangeFrom', 0))
    wind_width = comp.get('windRangeWidth', 0)
    if wind_width < 1:
        return f"{wind_est:.0f}°"
    else:
        return f"{wind_est:.0f}±{wind_width/2:.0f}°"


def create_node_label(node, best_type=None):
    """Create HTML-like label for a node with 4 compartments."""
    compartments = {c['type']: c for c in node['compartments']}
    
    # Extract time from timestamp
    timestamp = node.get('timestamp', '')
    if ' ' in timestamp:
        time_part = timestamp.split(' ')[1][:8]  # HH:MM:SS
    else:
        time_part = timestamp[:8] if len(timestamp) >= 8 else timestamp
    
    # Build HTML table
    html = '<<TABLE BORDER="0" CELLBORDER="1" CELLSPACING="0" CELLPADDING="2">'
    html += '<TR>'
    
    for type_name in TYPE_ORDER:
        comp = compartments.get(type_name)
        if comp is None:
            continue
        
        confidence = comp['confidence']
        is_best = (best_type == type_name)
        
        # Calculate background color
        base_color = TYPE_COLORS[type_name]
        if is_best:
            # Highlight best with full color and border
            bg_color = base_color
            border = ' BGCOLOR="{}" COLOR="red"'.format(base_color)
        else:
            # Fade color based on confidence
            intensity = confidence_to_intensity(confidence)
            # Make it lighter by blending with white
            r = int(int(base_color[1:3], 16) * confidence + 255 * (1-confidence))
            g = int(int(base_color[3:5], 16) * confidence + 255 * (1-confidence))
            b = int(int(base_color[5:7], 16) * confidence + 255 * (1-confidence))
            bg_color = f"#{min(255,r):02x}{min(255,g):02x}{min(255,b):02x}"
            border = f' BGCOLOR="{bg_color}"'
        
        wind_str = format_wind(comp)
        abbrev = TYPE_ABBREV[type_name]
        
        # Create cell content
        cell_content = f'<B>{abbrev}</B><BR/><FONT POINT-SIZE="8">{confidence:.2f}</FONT><BR/><FONT POINT-SIZE="7">{wind_str}</FONT>'
        
        html += f'<TD{border}>{cell_content}</TD>'
    
    html += '</TR>'
    html += f'<TR><TD COLSPAN="4"><FONT POINT-SIZE="8">{time_part}</FONT></TD></TR>'
    html += '</TABLE>>'
    
    return html


def visualize_mst_graph(data, output_file=None, max_nodes=100, show_low_prob_edges=False):
    """
    Create graphviz visualization of MST graph.
    
    Args:
        data: Parsed JSON data from MstGraphExporter
        output_file: Output file path (without extension; .pdf/.png will be added)
        max_nodes: Maximum nodes to show
        show_low_prob_edges: Whether to show edges with very low probability
    """
    nodes = {n['id']: n for n in data['nodes']}
    edges = data['edges']
    best_paths = data.get('bestPaths', {})
    
    # Create directed graph
    dot = Digraph(comment='MST Maneuver Graph')
    dot.attr(rankdir='TB')  # Top to bottom
    dot.attr('node', shape='plaintext')  # Use HTML labels
    
    # Limit nodes
    nodes_to_draw = list(nodes.keys())[:max_nodes]
    nodes_set = set(nodes_to_draw)
    
    # Add nodes
    for node_id in nodes_to_draw:
        node = nodes[node_id]
        best_type = best_paths.get(str(node_id))
        label = create_node_label(node, best_type)
        dot.node(str(node_id), label)
    
    # Collect best path edges for highlighting
    best_edge_keys = set()
    for edge in edges:
        if edge.get('isBestPath', False):
            key = (edge['from'], edge['to'], edge['fromType'], edge['toType'])
            best_edge_keys.add(key)
    
    # Group edges by (from_node, to_node) for simplicity
    # Only show best path edges and edges between same types
    edge_groups = {}
    for edge in edges:
        from_id = edge['from']
        to_id = edge['to']
        
        if from_id not in nodes_set or to_id not in nodes_set:
            continue
        
        is_best = edge.get('isBestPath', False)
        trans_prob = edge['transitionProbability']
        
        # Filter: show best path edges, same-type edges, or high probability edges
        same_type = edge['fromType'] == edge['toType']
        high_prob = trans_prob > 0.001
        
        if is_best or (same_type and high_prob) or show_low_prob_edges:
            key = (from_id, to_id)
            if key not in edge_groups:
                edge_groups[key] = []
            edge_groups[key].append(edge)
    
    # Add edges (simplified - one edge per node pair, prioritizing best path)
    for (from_id, to_id), edge_list in edge_groups.items():
        # Find best edge in this group
        best_edge = None
        for e in edge_list:
            if e.get('isBestPath', False):
                best_edge = e
                break
        
        if best_edge is None:
            # Use edge with highest probability
            best_edge = max(edge_list, key=lambda x: x['transitionProbability'])
        
        is_best = best_edge.get('isBestPath', False)
        trans_prob = best_edge['transitionProbability']
        from_type = best_edge['fromType']
        to_type = best_edge['toType']
        
        # Style based on best path
        if is_best:
            color = 'red'
            penwidth = '2.5'
            style = 'bold'
        else:
            # Color based on probability
            if trans_prob > 0.01:
                color = 'darkgreen'
            elif trans_prob > 0.001:
                color = 'gray40'
            else:
                color = 'gray80'
            penwidth = '1.0'
            style = 'solid'
        
        # Label shows type transition and probability
        label = f'{TYPE_ABBREV[from_type]}→{TYPE_ABBREV[to_type]}\\n{trans_prob:.1e}'
        
        dot.edge(str(from_id), str(to_id), 
                label=label,
                color=color, 
                penwidth=penwidth,
                style=style,
                fontsize='8',
                fontcolor=color)
    
    # Render
    if output_file:
        # Determine format from extension
        if '.' in output_file:
            base, fmt = output_file.rsplit('.', 1)
        else:
            base = output_file
            fmt = 'pdf'
        
        dot.render(base, format=fmt, cleanup=True)
        print(f"Saved visualization to {base}.{fmt}")
    else:
        # Try to display
        dot.view()
    
    return dot


def create_detailed_edge_graph(data, output_file=None, max_depth=10):
    """
    Create a more detailed graph showing all compartment-to-compartment edges.
    Each node compartment becomes its own graphviz node.
    
    This is useful for small subtrees to see the full inner graph structure.
    """
    nodes = {n['id']: n for n in data['nodes']}
    edges = data['edges']
    best_paths = data.get('bestPaths', {})
    
    # Filter to first N levels
    nodes_to_draw = [n for n in data['nodes'] if n['depth'] <= max_depth]
    nodes_set = {n['id'] for n in nodes_to_draw}
    
    dot = Digraph(comment='MST Detailed Inner Graph')
    dot.attr(rankdir='TB')
    
    # Create subgraph for each tree node (to keep compartments together)
    for node in nodes_to_draw:
        node_id = node['id']
        best_type = best_paths.get(str(node_id))
        
        with dot.subgraph(name=f'cluster_{node_id}') as c:
            c.attr(label=f"Node {node_id}")
            c.attr(style='rounded')
            
            for comp in node['compartments']:
                type_name = comp['type']
                comp_id = f"{node_id}_{type_name}"
                
                is_best = (best_type == type_name)
                color = TYPE_COLORS[type_name]
                
                if is_best:
                    style = 'filled,bold'
                    fillcolor = color
                    fontcolor = 'white'
                else:
                    style = 'filled'
                    # Lighter version
                    fillcolor = f"{color}40"  # 40 = 25% opacity in hex
                    fontcolor = 'black'
                
                label = f"{TYPE_ABBREV[type_name]}\\n{comp['confidence']:.2f}\\n{format_wind(comp)}"
                
                c.node(comp_id, label,
                      shape='box',
                      style=style,
                      fillcolor=fillcolor,
                      fontcolor=fontcolor,
                      fontsize='10')
    
    # Add edges between compartments
    for edge in edges:
        from_id = edge['from']
        to_id = edge['to']
        
        if from_id not in nodes_set or to_id not in nodes_set:
            continue
        
        from_comp_id = f"{from_id}_{edge['fromType']}"
        to_comp_id = f"{to_id}_{edge['toType']}"
        
        is_best = edge.get('isBestPath', False)
        trans_prob = edge['transitionProbability']
        
        # Only show significant edges
        if trans_prob < 0.0001 and not is_best:
            continue
        
        if is_best:
            color = 'red'
            penwidth = '2.0'
        else:
            # Color by probability
            if trans_prob > 0.01:
                color = 'darkgreen'
                penwidth = '1.5'
            elif trans_prob > 0.001:
                color = 'gray50'
                penwidth = '1.0'
            else:
                color = 'gray80'
                penwidth = '0.5'
        
        dot.edge(from_comp_id, to_comp_id,
                label=f'{trans_prob:.1e}',
                color=color,
                penwidth=penwidth,
                fontsize='7',
                fontcolor=color)
    
    if output_file:
        if '.' in output_file:
            base, fmt = output_file.rsplit('.', 1)
        else:
            base = output_file
            fmt = 'pdf'
        dot.render(base, format=fmt, cleanup=True)
        print(f"Saved detailed visualization to {base}.{fmt}")
    
    return dot


def main():
    if len(sys.argv) < 2:
        print("Usage: python mst_graph_visualizer_graphviz.py <input_json_file> [output_file] [--detailed]")
        print("\nOptions:")
        print("  input_json_file  - JSON file exported from MstGraphExporter")
        print("  output_file      - Output file (extension determines format: .pdf, .png, .svg)")
        print("  --detailed       - Create detailed compartment-level graph (for small graphs)")
        sys.exit(1)
    
    input_file = sys.argv[1]
    output_file = sys.argv[2] if len(sys.argv) > 2 and not sys.argv[2].startswith('--') else None
    detailed = '--detailed' in sys.argv
    
    print(f"Loading graph from {input_file}...")
    with open(input_file, 'r') as f:
        data = json.load(f)
    
    num_nodes = len(data['nodes'])
    num_edges = len(data['edges'])
    print(f"Loaded {num_nodes} nodes, {num_edges} edges")
    
    if detailed:
        print("Creating detailed compartment-level visualization...")
        create_detailed_edge_graph(data, output_file, max_depth=10)
    else:
        print("Creating tree visualization...")
        visualize_mst_graph(data, output_file, max_nodes=100)


if __name__ == '__main__':
    main()
