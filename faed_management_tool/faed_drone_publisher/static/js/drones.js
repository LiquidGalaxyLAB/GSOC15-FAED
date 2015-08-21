// Sets the map on all markers in the array.
function setAllMap(map) {
  for (var i = 0; i < markers.length; i++) {
    markers[i].setMap(map);
  }
}

// Removes the markers from the map, but keeps them in the array.
function clearMarkers() {
  setAllMap(null);
}

// Shows any markers currently in the array.
function showMarkers() {
  setAllMap(map);
}


// Add a marker to the map and push to the array.
function addMarker(location, map, image) {
  var marker = new google.maps.Marker({
    position: location,
    map: map,
    icon: image
  });
  markers.push(marker);

}



function loadHangars(){

var hangar = {}

var url = "http://localhost:8000/api/hangars/?format=json";

$.getJSON( url, function( data ) {
  var items = [];
  $.each( data, function( key, val ) {

    if (key == "results"){
      var results = val;
    $.each( results, function (key,val){
        hangar[val.id] = {
  center: new google.maps.LatLng(val.latitude, val.longitude),
  radius: val.radius
};

    })
    }
  });

});
console.log(hangar);
return hangar;
}


function loadDroppoints(){

var droppoint = {}

var url = "http://localhost:8000/api/droppoints/?format=json";

$.getJSON( url, function( data ) {
  var items = [];
  $.each( data, function( key, val ) {

    if (key == "results"){
      var results = val;

    $.each( results, function (key,val){
        console.log(val)
        droppoint[val.name] = {
  center: new google.maps.LatLng(val.latitude, val.longitude),
};

    })
    }
  });

});
console.log(droppoint)
return droppoint


}

function weatherStation(){


}