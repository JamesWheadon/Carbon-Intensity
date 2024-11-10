import { select, scaleLinear, max, axisBottom, axisLeft, scaleBand } from "d3";
import React, { useRef, useEffect } from "react";
import "./IntensityGraph.css"

function BarChart({ data }) {
    const svgRef = useRef();

    const margin = { top: 10, right: 0, bottom: 30, left: 60 };
    const width = 800 - margin.left - margin.right;
    const height = 300 - margin.top - margin.bottom;

    useEffect(() => {
        const svg = select(svgRef.current);
        const xScale = scaleBand().domain(data.map((d) => d.time)).range([0, width]);
        const yScale = scaleLinear().domain([0, max(data, (d) => d.intensity)]).range([height, 0]);

        svg.selectAll(".bar")
            .data(data)
            .enter()
            .append("rect")
            .attr("class", "bar")
            .attr("x", (d) => xScale(d.time))
            .attr("y", (d) => yScale(d.intensity))
            .attr("width", xScale.bandwidth())
            .attr("height", (d) => height - yScale(d.intensity))
            .attr("transform", `translate(${margin.left},${margin.top})`)
            .attr("fill", "steelblue");

        const xAxis = axisBottom(xScale);
        svg.append("g")
            .attr("class", "x-axis")
            .attr("transform", `translate(${margin.left},${height + margin.top})`)
            .call(xAxis);

        const yAxis = axisLeft(yScale);
        svg.append("g")
            .attr("class", "y-axis")
            .attr("transform", `translate(${margin.left},${margin.top})`)
            .call(yAxis);
        svg.append("text")
            .attr("class", "y-label")
            .attr("text-anchor", "middle")
            .attr("y", 0)
            .attr("x", 0 - (height / 2))
            .attr("dy", "1em")
            .attr("transform", "rotate(-90)")
            .attr("fill", "aqua")
            .text("gCO2/kWh");
    }, [data]);
    return (
        <div>
            <svg ref={svgRef} width={800} height={300}>
            </svg>
        </div>
    )
}

export default BarChart;
