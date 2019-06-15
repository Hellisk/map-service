var reader;
var counter = 0;

function checkFileAPI() {
    if (window.File && window.FileReader && window.FileList && window.Blob) {
        reader = new FileReader();
        return true;
    } else {
        alert('The File APIs are not fully supported by your browser. Fallback required.');
        return false;
    }
}

function readGPS_untransform(filepath) {
    reader.onload = function (e) {
        //alert('hi');
        console.log(counter);
        color = "#000000";
        console.log(color);
        counter += 1;
        output = e.target.result;
        //lines = output.split("\n")
        eval(output);
        //lert(lines[0]);
        points = [];
        var _taj;
        markers = [];
        var infoWindow = new AMap.InfoWindow({offset: new AMap.Pixel(0, -30)});

        function markerClick(e) {
            infoWindow.setContent(e.target.content);
            infoWindow.open(map, e.target.getPosition());
        }

        for (var i = 0; i < lines.length - 1; i++) {
            var _idx = i;
            line = lines[i];
            temp = line;
            points.push(new AMap.LngLat(parseFloat(temp[0]), parseFloat(temp[1])));
            points.push(new AMap.LngLat(parseFloat(temp[2]), parseFloat(temp[3])));

            polyline = new AMap.Polyline({
                path: [new AMap.LngLat(temp[0], temp[1]),
                    new AMap.LngLat(temp[2], temp[3])],
                strokeColor: color,
                strokeWeight: 1,
            });
            polyline.setMap(map);
            var marker = new AMap.Marker({
                position: [(temp[0] + temp[2]) / 2.0, (temp[1] + temp[3]) / 2.0],
                map: map
            });
            marker.content = String(i);
            marker.on('click', markerClick);
            marker.emit('click', {target: marker});
        }
        console.log('end');

    };
    reader.readAsText(filepath.files[0]);
}


function readGPS(filepath) {
    reader.onload = function (e) {
        //alert('hi');
        console.log(counter);
        if (counter == 0) {
            color = "#ff0000";
        } else if (counter == 1) {
            color = "#00ff00";
        } else if (counter == 2) {
            color = "#0000ff";
        } else if (counter == 3) {
            color = "#880000";
        } else if (counter == 4) {
            color = "#008800";
        } else if (counter == 5) {
            color = "#000088";
        }
        color = "#000000";
        console.log(color);
        counter += 1;
        output = e.target.result;
        //lines = output.split("\n")
        eval(output);
        //lert(lines[0]);
        points = [];
        var _taj;
        markers = [];
        for (var i = 0; i < lines.length - 1; i++) {
            var _idx = i;
            line = lines[i];
            temp = line;
            points.push(new AMap.LngLat(parseFloat(temp[0]), parseFloat(temp[1])));
            points.push(new AMap.LngLat(parseFloat(temp[2]), parseFloat(temp[3])));
            if (i % 200 == 0 || i == lines.length - 2) {
                //if(true){
                //if(i == 0)
                //    continue;
                AMap.convertFrom(points, "gps", function (status, result) {
                    traj = [];
                    for (var j = 0; j < result.locations.length; j++) {
                        //x = result.locations[i].lng;
                        //y = result.locations[i].lat;
                        //marker = new AMap.Marker({
                        //icon: "http://webapi.amap.com/theme/v1.3/markers/n/mark_b.png",
                        //position: [x, y]
                        //});
                        //marker.setMap(map);

                        x = result.locations[j].lng;
                        y = result.locations[j].lat;
                        traj.push(new AMap.LngLat(x, y));
                        if (j % 2 == 1) {
                            polyline = new AMap.Polyline({
                                path: traj,
                                strokeColor: color,
                                strokeWeight: 2,
                            });
                            polyline.setMap(map);
                            //alert(traj)
                            markers.push([_idx + ':' + j, (traj[0]['lng'] + traj[1]['lng']) / 2.0, (traj[0]['lat'] + traj[1]['lat']) / 2.0]);
                            //console.log(traj);
                            //console.log((traj[0]+traj[2])/2.0);
                            //console.log((traj[1]+traj[3])/2.0);
                            _traj = traj;
                            traj = [];
                        }
                    }
                });
                points = []
            }
        }
        console.log('end');
        /*
        for( i = 0; i < markers.length-1; i++){
            var marker = new AMap.Marker({
                position: [markers[i][1], markers[i][2]],
                map: map
            });
            marker.content = String(markers[i][0]);
            marker.on('click', markerClick);
            marker.emit('click', {target: marker});
        }
        var infoWindow = new AMap.InfoWindow({offset: new AMap.Pixel(0, -30)});
        function markerClick(e) {
            infoWindow.setContent(e.target.content);
            infoWindow.open(map, e.target.getPosition());
        }
        */

    };
    reader.readAsText(filepath.files[0]);
}

function readText(filepath) {

    reader.onload = function (e) {
        output = e.target.result;
        //lines = output.split("\n")
        eval(output);
        alert(lines[0]);
        points = [];
        gid = [];
        for (var i = 0; i < lines.length - 1; i++) {
            //marker = new AMap.Marker({
            //icon: "http://webapi.amap.com/theme/v1.3/markers/n/mark_b.png",
            //position: [116.405467, 39.907761]
            //});
            //marker.setMap(map);
            line = lines[i];
            temp = line.split(",");
            points.push(new AMap.LngLat(parseFloat(temp[1]), parseFloat(temp[2])));
            points.push(new AMap.LngLat(parseFloat(temp[3]), parseFloat(temp[4])));
            gid.push(parseInt(temp[0]))
        }
        AMap.convertFrom(points, "gps", function (status, result) {
            for (var i = 0; i < result.locations.length; i += 2) {
                x1 = result.locations[i].lng;
                y1 = result.locations[i].lat;
                x2 = result.locations[i + 1].lng;
                y2 = result.locations[i + 1].lat;
                var grid_points = [];
                grid_points.push(new AMap.LngLat(x1, y1));
                grid_points.push(new AMap.LngLat(x2, y1));
                grid_points.push(new AMap.LngLat(x2, y2));
                grid_points.push(new AMap.LngLat(x1, y2));
                polygon = new AMap.Polygon({
                    path: grid_points,
                    strokeColor: "#ff0000",
                    fillOpacity: 0
                });
                polygon.setMap(map);
                label = new AMap.Marker({
                    position: new AMap.LngLat((x1 + x2) / 2, (y1 + y2) / 2),
                    content: "<div>" + gid[parseInt(i / 2)] + "</div>"
                });
                label.setMap(map)
            }
        })
        //alert(points[0])
        //alert(lines[0])
    };
    reader.readAsText(filepath.files[0]);
}
