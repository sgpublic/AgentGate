import axios from "axios";

const isDev = (process.env.NODE_ENV === "development")

export const AgentGateAPI =  axios.create({
    baseURL: isDev ? (process.env.REACT_APP_BASE_API ?? "http://localhost:1180/agent-gate/web") : "../",
    validateStatus: (status) => status !== 404
})
