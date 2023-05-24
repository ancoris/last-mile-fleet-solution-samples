window.addEventListener("DOMContentLoaded", (e) => {
    const deliveryVehicleIdInput = document.getElementById("delivery-vehicle-id-input");

    const vehicleRows = document.querySelectorAll("ul.vehicleChooser li.mdc-deprecated-list-item");
    vehicleRows.forEach(row => {
        const radio = row.querySelector("input.mdc-radio__native-control");
        row.addEventListener('click', (e) => {
            // Material UI is supposed to do this for us, but it doesn't seem to work. This is fine for a demo / POC but ideally we should debug it properly.
            radio.checked = true;
            const event = new Event('change');
            radio.dispatchEvent(event);
        });
        radio.addEventListener('change', (e) => {
            if (radio.checked) {
                deliveryVehicleIdInput.focus();
                deliveryVehicleIdInput.value = radio.dataset.uiname;
                deliveryVehicleIdInput.dispatchEvent(new KeyboardEvent('keyup',{'key':'Enter'}));
                deliveryVehicleIdInput.blur();
                radio.focus();
            }
        });
    });

    window.setInterval(updateDeliveryVehicleTracking, 1000 * 60);
    updateDeliveryVehicleTracking();
});

const updateDeliveryVehicleTracking = () => {
    fetch("/fleet/vehicles")
        .then((response) => response.json())
        .catch((e) => {
            console.error('window.deliveryVehiclesJson defaulted to empty array due to json parse error', e);
            return [];
        }).then((deliveryVehiclesJson) => {
            console.log(deliveryVehiclesJson);
            window.deliveryVehiclesJson = deliveryVehiclesJson;
            window.deliveryVehiclesJson.forEach((deliveryVehicle, i) => {
                console.log(deliveryVehicle, i);
                if (deliveryVehicle.lastLocation_ && deliveryVehicle.lastLocation_.location_) {
                    const marker = new google.maps.Marker({map: window.deliveryVehicleTrackingApp.mapView.map, icon: {
                        path: 'M12 2L4.5 20.29l.71.71L12 18l6.79 3 .71-.71z',
                        fillOpacity: 0.9,
                        scale: 1.2,
                        fillColor: '#bababa',
                        anchor: {x: 12, y: 12},
                      }});
                    marker.setPosition({
                        lat: (i * 0.0005) + deliveryVehicle.lastLocation_.location_.latitude_,
                        lng: deliveryVehicle.lastLocation_.location_.longitude_
                    });
                    const lastUpdate = deliveryVehicle.lastLocation_.serverTime_ ? 
                        moment(deliveryVehicle.lastLocation_.serverTime_.seconds_ * 1000).fromNow() : "Unknown";
                    console.log('Setting title to', lastUpdate);
                    marker.setTitle(lastUpdate);
                } else {
                    console.log('No location for', deliveryVehicle);
                }
            });          
        });
};