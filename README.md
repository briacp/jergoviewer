## jErgoViewer

![jErgoViewer screenshot](https://raw.github.com/briacp/jergoviewer/master/briac.net/www/img/screenshot_02.png)

jErgoViewer is an interface for Kettler training equipement. It let you visualize your training along a Google Street View path, as if you were exercising outside.

You can use any KML file with a PolyLine path plotted. I recommand using [GPSies](http://www.gpsies.com/trackList.do) as you can export tracks with points spaced evenly (every 20 meters), which allows for a better StreetView display.

![jErgoViewer screenshot](https://raw.github.com/briacp/jergoviewer/master/briac.net/www/img/screenshot_01.png)

jErgoViewer screen is designed to be properly displayed on a TV and features :
- the current pulse
- the time elapsed or remaining (depending on the selected program)
- the current pace (mn/km)
- the total distance


## Libraries used
jErgoViewer uses 
- [jErgometer](http://trac.jergometer.org/), to capture and process data coming from the exercise bike.
- Icons from [Iconic](http://somerandomdude.com/work/iconic/) 
- [sprintf.js](https://github.com/alexei/sprintf.js) library
- [share.js](https://github.com/carrot/share-button) library
- [jquery.sparkline](http://omnipotent.net/jquery.sparkline) library
- [jquery 2.10](http://www.jquery.com)
- Google [Maps](https://developers.google.com/maps/documentation/javascript/) and [StreetView](https://developers.google.com/maps/documentation/streetview/) APIs
           
