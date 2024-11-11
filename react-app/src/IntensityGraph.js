import { select, scaleLinear, max, axisBottom, axisLeft, timeFormat, timeHour, scaleTime } from "d3";
import React, { useRef, useEffect } from "react";
import "./IntensityGraph.css"

function BarChart({ data }) {
    const svgRef = useRef();

    const margin = { top: 10, right: 20, bottom: 30, left: 60 };
    const width = 1200 - margin.left - margin.right;
    const height = 500 - margin.top - margin.bottom;

    useEffect(() => {
        const svg = select(svgRef.current);
        const xScale = scaleTime().domain([data[0].time, new Date(data[data.length - 1].time + 30 * 60000)]).range([0, width]);
        const yScale = scaleLinear().domain([0, max(data, (d) => d.intensity)]).range([height, 0]);

        svg.selectAll(".bar")
            .data(data)
            .enter()
            .append("rect")
            .attr("class", "bar")
            .attr("x", (d) => xScale(d.time))
            .attr("y", (d) => yScale(d.intensity))
            .attr("width", width / data.length * 0.95)
            .attr("height", (d) => height - yScale(d.intensity))
            .attr("transform", `translate(${margin.left},${margin.top})`)
            .attr("fill", "steelblue");

        const xAxis = axisBottom(xScale)
        .ticks(timeHour.every(3))
        .tickFormat(d => {
            return timeFormat("%H:%M")(d) === "00:00" 
                ? timeFormat("%b %d")(d)
                : timeFormat("%H:%M")(d);
        });

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
    }, [data, width, height, margin.top, margin.left]);
    return (
        <div>
            <svg ref={svgRef} width={width + margin.left + margin.right} height={height + margin.top + margin.bottom}>
            </svg>
        </div>
    )
}

export default BarChart;
