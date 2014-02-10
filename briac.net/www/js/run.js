/*jshint bitwise:true, browser:true, camelcase:true, curly:true, devel:false, eqeqeq:false, forin:true, immed:true, indent:4, newcap:true, noarg:true, noempty:true, nonew:true, prototypejs:true, quotmark:true, regexp:false, strict:true, trailing:true, undef:true, unused:true */
/*global google:false, sprintf:false */
$(function () {
    "use strict";
    var delay = 500,
        runMap, marker, runTimer, beats = [], speeds = [], alts = [], powers = [];

    $("#share").share({
        url: "http://briac.net/jergoviewer",
        text: "StreetView in you living room while you exercise"
    });

    $("#stopRun").click(function ()    { window.clearInterval(runTimer); });
    $("#showSparks").click(function () { $("#sparks").toggle(); });
    $("#showInfo").click(function ()   { $("#info").toggle(); });
    $("#showMap").click(function ()    { $("#map").toggle(); });

    $.getJSON("run.json", function (data) {
        initRun(data);
    });

    function finishRun() {
        window.clearInterval(runTimer);
    }

    function updateRun(d) {
        if (!d) {
            finishRun();
        }

        // Refresh the sparklines ==============================================
        speeds.push(d.speed / 10);
        beats.push(d.pulse === 0 ? null : d.pulse);
        alts.push(Math.floor(d.point.alt));
        powers.push(d.destPower);

        $("#sparkSpeed").sparkline(speeds,   { width: "100%", defaultPixelsPerValue: 1, fillColor: "rgba(68, 68, 238, .5)", tooltipPrefix: "Speed: ",    tooltipSuffix: "km/h"  });
        $("#sparkHeart").sparkline(beats,    { width: "100%", defaultPixelsPerValue: 1, fillColor: "rgba(238, 68, 68, .5)", tooltipPrefix: "Pulse: ",    tooltipSuffix: "bpm" });
        $("#sparkAltitude").sparkline(alts,  { width: "100%", defaultPixelsPerValue: 1, fillColor: "rgba(68, 238, 68, .5)", tooltipPrefix: "Altitude: ", tooltipSuffix: "m" });
        $("#sparkPower").sparkline(powers,   { width: "100%", defaultPixelsPerValue: 1, fillColor: "rgba(255, 22, 22, .5)", tooltipPrefix: "Power: ",    tooltipSuffix: "W" });

        // Refresh the map  ====================================================
        var currentLatLng = new google.maps.LatLng(d.point.lat, d.point.lon);
        marker.setPosition(currentLatLng);
        runMap.panTo(currentLatLng);

        // Refresh the interface ===============================================
        $("#street").attr("src", d.img);
        $("#heartbeat").html(d.pulse);
        $("#distance").html(sprintf("%.2f", d.calcDistance / 1000) + "km");
        $("#time").html(d.time + "<br/><hr/>" + ( isFinite(d.pace) ? d.pace + "mn/km" : "--" ));

        // Refresh the info ====================================================
        $("#info").html(
            "<dl>" +
            "<dl>pedalRpm</dl><dd>" + d.pedalRpm + "</dd>" +
            "<dl>distance</dl><dd>" + d.distance + "</dd>" +
            "<dl>speed</dl><dd>" + d.speed + "</dd>" +
            "<dl>destPower</dl><dd>" + d.destPower + "</dd>" +
            "<dl>energy</dl><dd>" + d.energy + "</dd>" +
            "<dl>p.lat</dl><dd>" + sprintf("%.4f", d.point.lat) + "</dd>" +
            "<dl>p.lon</dl><dd>" + sprintf("%.4f", d.point.lon) + "</dd>" +
            "<dl>p.alt</dl><dd>" + sprintf("%.2f", d.point.alt) + "</dd>" +
            "<dl>p.dist</dl><dd>" + d.point.distance + "</dd>" +
            "<dl>calcDistance</dl><dd>" + sprintf("%.2f", d.calcDistance) + "</dd>" +
            "<dl>deltaAltitude</dl><dd>" + d.deltaAltitude + "</dd>" +
            "</dl>");
    }


    function initRun(data) {

        // Map stuff =======================================
        var currentLatLng = new google.maps.LatLng(data.point.lat, data.point.lon);
        runMap = new google.maps.Map(document.getElementById("map"), {
            center: currentLatLng,
            mapTypeControl: false,
            streetViewControl: false,
            scaleControl: false,
            panControl: false,
            overviewMapControl: false,
            zoomControl: false,
            zoom: 14
        });
        marker = new google.maps.Marker({
            position: currentLatLng,
            map: runMap
        });


        // Kinetic stuff ====================================
        /*
        var stage = new Kinetic.Stage({
            container: "street",
            width:  600,
            height: 600
        });
        var layer = new Kinetic.Layer();

        var imageObj = new Image();
        imageObj.onload = function() {
            var streetView = new Kinetic.Image({
                x: 0,
                y: 0,
                image: imageObj,
                width: 600,
                height:  600
            });
            layer.add(streetView);
            stage.add(layer);
        };
        imageObj.src = "kml2sv/ParisMarathon2012.kml/SV_48.86323940-2.296139000.jpg";
        */

        // Timer stuff ===================================
        runTimer = window.setInterval(function () {
            $.getJSON("run.json", function (d) {
                updateRun(d);
            });
        }, delay);
    }


});
