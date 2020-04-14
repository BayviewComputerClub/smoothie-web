// Import files to be bundled.
import React from "react";
import ReactDOM from "react-dom";
import Header from "./Header/Header";

window.React = React;
window.ReactDOM = ReactDOM;


// Make the components accessible to the page.
window.smoothie_components = {
    Header
};

console.log("smoothie-web-js loaded 🎉");