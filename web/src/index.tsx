import React, {lazy} from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import i18next from "i18next";
import I18nextBrowserLanguageDetector from "i18next-browser-languagedetector";
import {initReactI18next} from "react-i18next";
import {resources} from "./react-i18next/resources";

i18next.use(I18nextBrowserLanguageDetector)
    .use(initReactI18next)
    .init({
        resources,
        fallbackLng: "zh",
        lng: navigator.language,
    })

const App = lazy(() => import("./App"))

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
