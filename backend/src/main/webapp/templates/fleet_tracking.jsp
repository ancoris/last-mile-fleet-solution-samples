<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<!DOCTYPE html>
<html lang="en-US">

<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <link rel="stylesheet" href="./main.css">
  <link rel="stylesheet" href="./shipment_tracking.css">
  <link rel="stylesheet" href="./fleet_tracking_jsp.css">
  <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Material+Icons|Material+Icons+Outlined">
  <link rel="stylesheet" href="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.css">
  <title>Fleet Tracking</title>
  <%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
  <%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
</head>

<body class="ancoris">
  <!-- header bar -->
  <header class="mdc-top-app-bar mdc-top-app-bar--fixed mdc-elevation--z3 mdc-theme--surface"
    style="background-color: white !important; color: #3c4043 !important;">
    <div class="mdc-top-app-bar__row">
      <section class="mdc-top-app-bar__section mdc-top-app-bar__section--align-start">
        <button id="menu-button"
          class="mdc-icon-button material-icons mdc-top-app-bar__navigation-icon mdc-theme--on-surface">
          <div class="mdc-icon-button__ripple"></div>
          menu
        </button>
        <span role="heading" aria-level="1" class="mdc-top-app-bar__title"
          style=" font-family: 'Google Sans', 'Roboto', sans-serif !important; font-weight: 400;">Fleet Tracking</span>
      </section>
    </div>
  </header>

  <!-- menu -->
  <aside class="mdc-drawer mdc-drawer--modal">
    <div class="mdc-drawer__header mdc-elevation--z2">
      <h3 class="mdc-drawer__title"
        style=" font-family: 'Google Sans', 'Roboto', sans-serif !important; font-weight: 400; margin-top: 0.25rem">
        Follow My Parcel</h3>
    </div>
    <div class=" mdc-drawer__content">
      <nav class="mdc-deprecated-list">
        <a class="mdc-deprecated-list-item" href="#" aria-current="page" tabindex="0" id="show-options-button">
          <span class="mdc-deprecated-list-item__ripple"></span>
          <i class="material-icons mdc-deprecated-list-item__graphic" aria-hidden="true">settings</i>
          <span class="mdc-deprecated-list-item__text">Options</span>
        </a>
        <a class="mdc-deprecated-list-item" href="#" aria-current="page" tabindex="0" id="reset-button">
          <span class="mdc-deprecated-list-item__ripple"></span>
          <i class="material-icons mdc-deprecated-list-item__graphic" aria-hidden="true">restart_alt</i>
          <span class="mdc-deprecated-list-item__text">Reset</span>
        </a>
      </nav>
    </div>
  </aside>
  <div class="mdc-drawer-scrim"></div>

  <main class="mdc-top-app-bar--fixed-adjust" style="height:calc(100% - 56px - 64px)">
    <div id="options-container"></div>

    <div class="grid">
        <div class="grid__col grid__col--vehicles">
            <h3 role="heading" id="vehicleChooserHeading" aria-level="1" style=" font-family: 'Google Sans', 'Roboto', sans-serif !important; font-weight: 400; margin-top: 0.25rem">Fleet of Vehicles (${fn:length(deliveryVehicles)})</h3>
            <ul class="vehicleChooser mdc-deprecated-list" role="radiogroup">
              <c:forEach items="${deliveryVehicles}" var="vehicle" varStatus="deliveryVehiclesStatus">
                <c:catch var="exception">
                  <c:set var="nameParts" value="${fn:split(vehicle.name, '/')}" />
                  <c:set var="uiName" value="${nameParts[fn:length(nameParts) - 1]}" />
                  <li class="mdc-deprecated-list-item" id="row_${uiName}" role="radio" aria-checked='<c:choose><c:when test="${deliveryVehiclesStatus.getIndex() == 0}">true</c:when><c:otherwise>false</c:otherwise></c:choose>' tabindex='<c:choose><c:when test="${deliveryVehiclesStatus.getIndex() == 0}">0</c:when><c:otherwise>-1</c:otherwise></c:choose>'>
                    <span class="mdc-deprecated-list-item__ripple"></span>
                    <span class="mdc-deprecated-list-item__graphic">
                        <div class="mdc-radio">
                            <input class="mdc-radio__native-control"
                                type="radio"
                                id="list-radio-item-${vehicle.name}"
                                name="deliveryVehicle"
                                value="${vehicle.name}"
                                data-name="${vehicle.name}"
                                data-uiname="${uiName}"
                                <c:choose><c:when test="${deliveryVehiclesStatus.getIndex() == 0}">checked</c:when></c:choose> />
                            <div class="mdc-radio__background">
                                <div class="mdc-radio__outer-circle"></div>
                                <div class="mdc-radio__inner-circle"></div>
                            </div>
                        </div>
                    </span>
                    <label class="mdc-deprecated-list-item__text" for="list-radio-item-${vehicle.name}">${uiName}</label>
                  </li>
                </c:catch>
              </c:forEach>
            </ul>
          </div>

      <div class="grid__col grid__col--info">
        <div class="input-container">
          <div class="mdc-text-field mdc-text-field--outlined mdc-text-field--with-trailing-icon"
            id="delivery-vehicle-text-field">
            <input class="mdc-text-field__input" id="delivery-vehicle-id-input">
            <div class="mdc-notched-outline">
              <div class="mdc-notched-outline__leading"></div>
              <div class="mdc-notched-outline__notch">
                <label for="delivery-vehicle-id-input" class="mdc-floating-label"
                  id="delivery-vehicle-id-input-label">Delivery Vehicle ID</label>
              </div>
              <div class="mdc-notched-outline__trailing"></div>
            </div>
            <i class="material-icons mdc-text-field__icon mdc-text-field__icon--trailing" id="tracking-button"
              tabindex="0" role="button">forward</i>
          </div>
        </div>
        <div id="delivery-vehicle-id-error"></div>

        <div class="details-container">
          <div id="loading">Loading...</div>

          <div class="mdc-tab-bar" role="tablist">
            <div class="mdc-tab-scroller">
              <div class="mdc-tab-scroller__scroll-area">
                <div class="mdc-tab-scroller__scroll-content">
                  <button class="mdc-tab mdc-tab--active" role="tab" aria-selected="true" tabindex="0">
                    <span class="mdc-tab__content">
                      <span class="mdc-tab__text-label">Tasks</span>
                    </span>
                    <span class="mdc-tab-indicator mdc-tab-indicator--active">
                      <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                    </span>
                    <span class="mdc-tab__ripple"></span>
                  </button>
                  <button class="mdc-tab" role="tab" aria-selected="false" tabindex="-1">
                    <span class="mdc-tab__content">
                      <span class="mdc-tab__text-label">Details</span>
                    </span>
                    <span class="mdc-tab-indicator">
                      <span class="mdc-tab-indicator__content mdc-tab-indicator__content--underline"></span>
                    </span>
                    <span class="mdc-tab__ripple"></span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <div class="mdc-data-table shipments-table-container" style="display:flex">
            <div class="mdc-data-table__table-container">
              <table class="mdc-data-table__table shipments-table" style="width:250px">
                <thead>
                  <tr class="mdc-data-table__header-row">
                    <th class="mdc-data-table__header-cell mdc-data-table__header-cell--numeric" role="columnheader"
                      scope="col" style="width:36px">#</th>
                    <th class="mdc-data-table__header-cell" role="columnheader" scope="col">Tracking ID</th>
                    <th class="mdc-data-table__header-cell" style="width:80px" role="columnheader" scope="col">Type</th>
                    <th class="mdc-data-table__header-cell" style="width:56px" role="columnheader" scope="col">Status</th>
                    <th class="mdc-data-table__header-cell" style="width:80px" role="columnheader" scope="col">Time</th>
                  </tr>
                </thead>
                <tbody class="mdc-data-table__content" id="task-table-body">

                </tbody>
                <tr class="mdc-data-table__row task-table-row" id="table-row-template" style="display:none">
                  <td class="mdc-data-table__cell mdc-data-table__cell--numeric stop_index"></td>
                  <td class="mdc-data-table__cell tracking_id"></td>
                  <td class="mdc-data-table__cell type"></td>
                  <td class="mdc-data-table__cell status"></td>
                  <td class="mdc-data-table__cell time"></td>
                </tr>
              </table>
            </div>
          </div>

          <table class="shipment-details-table" style="display:none">
            <tbody>
              <tr>
                <td class="shipment-details-table__label">Delivery Vehicle ID</td>
                <td class="shipment-details-table__value" id="delivery-vehicle-id-value"></td>
              </tr>

              <tr>
                <td class="shipment-details-table__label"># Stops Remaining</td>
                <td class="shipment-details-table__value" id="stops-remaining-value"></td>
              </tr>

              <tr>
                <td class="shipment-details-table__label">Navigation Status</td>
                <td class="shipment-details-table__value" id="navigation-status-value"></td>
              </tr>

              <tr>
                <td class="shipment-details-table__label">ETA</td>
                <td class="shipment-details-table__value" id="eta-value"></td>
              </tr>

              <tr>
                <td class="shipment-details-table__label">Distance to Next Stop</td>
                <td class="shipment-details-table__value" id="distance-value"></td>
              </tr>

              <tr>
                <td class="shipment-details-table__label">Tasks</td>
                <td class="shipment-details-table__value" id="completed-tasks-value"></td>
              </tr>

            </tbody>
          </table>
        </div>
      </div>
      <div class="grid__col grid__col--map">
        <div id="vehicleSpeedingInfo" class="speeding-none">Speeding data currently unavailable</div>
        <div id="map_canvas"></div>
      </div>
    </div>
  </main>
  <div class="mdc-card" id="task-details-popup-template" style="display:none;margin:1rem">
    <div style="padding: 1rem">
      <h2 class="mdc-typography mdc-typography--headline6" style="margin:0">Task <span class="task-index"></span>: <span
          class="task-status"></span></h2>
      <p class="mdc-typography mdc-typography--subtitle2" style="margin:0">Tracking ID: <span
          class="tracking-id"></span></p>
      <p class="mdc-typography mdc-typography--body1" style="margin:0"><span class="address"></span></p>
    </div>
  </div>
</body>
<script src="options_fleet.js" defer></script>
<script src="fleet_tracking_jsp.js" defer></script>
<script src="fleet_tracking_jsp_ancoris.js" defer></script>
<script src="config.js"></script>
<script src="https://maps.googleapis.com/maps/api/js?key=${API_KEY}&callback=initializeJourneySharing&v=beta&libraries=journeySharing" defer></script>
<script src="https://unpkg.com/material-components-web@latest/dist/material-components-web.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.3.1/moment.min.js" integrity="sha512-XmlzdZO9UbgsZxntPxZS6FG5uPMcdWmY+VaYvw7ENvaBwab9Wk3JK76ivbjjSh+EuD4Wglnixj1LFSdjaEHq3A==" crossorigin="anonymous" referrerpolicy="no-referrer"></script>
<!-- Attach Material Design classes to elements. -->
<script>
    window.addEventListener("DOMContentLoaded", () => {
        const topAppBar = mdc.topAppBar.MDCTopAppBar.attachTo(document.querySelector('.mdc-top-app-bar'));
        const drawer = mdc.drawer.MDCDrawer.attachTo(document.querySelector('.mdc-drawer'));
        const tabBar = mdc.tabBar.MDCTabBar.attachTo(document.querySelector('.mdc-tab-bar'));
        mdc.textField.MDCTextField.attachTo(document.querySelector('.mdc-text-field'));
        topAppBar.listen('MDCTopAppBar:nav', () => {
            drawer.open = !drawer.open;
        });
        document.querySelector('.mdc-drawer .mdc-deprecated-list').addEventListener('click', (e) => {
            drawer.open = false;
        });
        tabBar.listen('MDCTabBar:activated', (d) => {
            switchTab(d.detail.index);
        });
        document.querySelectorAll('.mdc-radio').forEach(el => {
            const radio = new mdc.radio.MDCRadio(el);
        });
        const list = new mdc.list.MDCList(document.querySelector('.mdc-deprecated-list'));
        const listItemRipples = list.listElements.map((listItemEl) => new mdc.ripple.MDCRipple(listItemEl));
    });
</script>

</html>
