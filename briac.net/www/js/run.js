/*jshint bitwise:true, browser:true, camelcase:true, curly:true, devel:false, eqeqeq:false, forin:true, immed:true, indent:4, newcap:true, noarg:true, noempty:true, nonew:true, prototypejs:true, quotmark:true, regexp:false, strict:true, trailing:true, undef:true, unused:true */
/*global google:false, sprintf:false */
$(function () {
    "use strict";
    var //heartBeat = 36,
    delay = 500,
        currentStepIndex = 0,
        lastDistance = 0,
        runMap,
        marker,
        runTimer,
        heartState = true;

    $("#stopRun").click(function () {
        window.clearInterval(runTimer);
    });
    $("#showInfo").click(function () {
        $("#info").toggle();
    });
    $("#showMap").click(function () {
        $("#map").toggle();
    });

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

        var  currentLatLng = new google.maps.LatLng(d.point.lat, d.point.lon);
        marker.setPosition(currentLatLng);
        runMap.panTo(currentLatLng);

        $("#street").attr("src", d.img);
        $("#heartbeat").html(d.pulse);
        $("#distance").html(sprintf("%.2f", d.calcDistance / 1000) + "&nbsp;km");
        $("#time").html(d.time);

        $("#info").html(
            "<dl>" +
            "<dl>pedalRpm</dl><dd>" + d.pedalRpm + "</dd>" +
            "<dl>distance</dl><dd>" + d.distance + "</dd>" +
            "<dl>speed</dl><dd>" + d.speed + "</dd>" +
            "<dl>destPower</dl><dd>" + d.destPower + "</dd>" +
            "<dl>energy</dl><dd>" + d.energy + "</dd>" +
            "<dl>p.lat</dl><dd>" + sprintf("%.4f",d.point.lat) + "</dd>" +
            "<dl>p.lon</dl><dd>" + sprintf("%.4f",d.point.lon) + "</dd>" +
            "<dl>p.alt</dl><dd>" + sprintf("%.2f",d.point.alt) + "</dd>" +
            "<dl>p.dist</dl><dd>" + d.point.distance + "</dd>" +
            "<dl>calcDistance</dl><dd>" + sprintf("%.2f", d.calcDistance) + "</dd>" +
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
            panControl:false,
            overviewMapControl:false,
            zoomControl: false,
            zoom: 14
        });
        marker = new google.maps.Marker({
            position: currentLatLng,
            map: runMap
            //title: "Hello World!"
        });
        //google.maps.event.addDomListener(window, 'load', initialize);


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
            heartState = heartState ? false : true;

            $.getJSON("run.json", function (d) {
                updateRun(d);
            });
        }, delay);
    }


});

