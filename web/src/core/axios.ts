import axios from "axios";

const isDev = import.meta.env.DEV

export const AgentGateAPI =  axios.create({
    baseURL: isDev ? (import.meta.env.VITE_BASE_API ?? "http://localhost:1180") : "/",
    validateStatus: (status) => status !== 404
})
