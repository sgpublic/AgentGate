import React, {Dispatch, FC, SetStateAction, useEffect, useRef, useState} from "react";
import {Helmet} from "react-helmet";
import {useTranslation} from "react-i18next";
import {Form, Card, FormItem, Input, FormSubmit, FormHelpers, Alert} from "@hi-ui/hiui";
import {TFunction} from "i18next";
import {AgentGateAPI} from "./core/axios";

const LoginPage: FC = () => {
    const {t} = useTranslation()

    const formRef = useRef<FormHelpers>(null)
    const [loginDoing, setLoginDoing] = useState(false)
    const [alert, setAlert] = useState<string | null>(null)

    const [logo, setLogo] = useState<string | null>(null)
    const [title, setTitle] = useState<string>("AgentGate")
    useEffect(() => {
        getInfo(setLogo, setTitle)
    }, [])
    useEffect(() => {
        document.title = t("login_title") + title
    }, [title]);

    return (
        <div style={{
            display: "flex",
            alignItems: "center",
            flexDirection: "column",
            height: "100vh",
        }}>
            {
                logo !== null && (
                    <div>
                        <Helmet>
                            <link rel={"icon"} href={logo} />
                            <link rel={"apple-touch-icon"} href={logo} />
                        </Helmet>
                        <img
                            src={logo ?? ""}
                            alt={"logo"}
                            style={{
                                width: 60,
                                height: 60,
                                marginTop: 36,
                                marginBottom: 24,
                            }}/>
                    </div>
                )
            }
            {
                logo === null && (
                    <div style={{height: 20}} />
                )
            }
            <div style={{
                fontSize: 20,
            }}>{t("login_title") + title}</div>
            {
                alert !== null && (
                    <Alert
                        title={<div>{alert}</div>}
                        closeable={true}
                        type={"danger"}
                        onClose={() => {
                            setAlert(null)
                        }}
                        style={{
                            width: 310,
                            marginTop: 12,
                        }}/>
                )
            }
            <Card
                style={{
                    width: 310,
                    marginTop: alert ? 0 : 24,
                }}>
                <Form
                    initialValues={{
                        username: process.env.REACT_APP_ADMIN_USERNAME ?? "",
                        password: process.env.REACT_APP_ADMIN_PASSWORD ?? ""
                    }}
                    labelWidth={80}
                    rules={{
                        username: [
                            {
                                required: true,
                                type: "string",
                                message: t("login_username_empty"),
                            },
                        ],
                        password: [
                            {
                                required: true,
                                type: "string",
                                message: t("login_password_empty"),
                            },
                        ],
                    }}
                    innerRef={formRef}>
                    <FormItem
                        field={"username"}
                        label={t("login_username")}
                        labelPlacement={"top"}
                        showColon={false}>
                        <Input disabled={loginDoing}/>
                    </FormItem>
                    <FormItem
                        field={"password"}
                        label={t("login_password")}
                        labelPlacement={"top"}
                        showColon={false}>
                        <Input type={"password"} disabled={loginDoing}/>
                    </FormItem>
                    <FormItem labelPlacement={"top"}>
                        <FormSubmit
                            type={"primary"}
                            onClick={(value, _) => {
                                if (value == null) {
                                    return
                                }
                                doLogin(
                                    value["username"], value["password"],
                                    setAlert, setLoginDoing,
                                    t, "/"
                                )
                            }}
                            style={{
                                width: "100%"
                            }}
                            loading={loginDoing}
                            disabled={loginDoing}>{
                            t("login_action")
                        }</FormSubmit>
                    </FormItem>
                </Form>
            </Card>
        </div>
    )
}

function doLogin(
    username: string, password: string,
    showAlert: Dispatch<SetStateAction<string | null>>,
    setLoading: Dispatch<SetStateAction<boolean>>,
    t: TFunction<"translation", undefined>,
    home: string,
) {
    setLoading(true)
    setTimeout(() => {
        AgentGateAPI.post("/api/login", {
            username: username,
            password: password,
        }).then(value => {
            if (value.data["code"] !== 200) {
                showAlert(t("login_failed") + value.data["message"])
                return
            }
            setLoading(false)
            window.location.href = home
        }).catch(error => {
            showAlert(t("login_failed") + error.message)
        }).finally(() => {
            setLoading(false)
        })
    }, 500)
}

function getInfo(
    setLogo: Dispatch<SetStateAction<string | null>>,
    setTitle: Dispatch<SetStateAction<string>>,
) {
    setTimeout(() => {
        AgentGateAPI.get("/api/info").then(value => {
            setLogo(value.data["serviceLogo"])
            setTitle(value.data["serviceName"])
        }).catch(error => {
            setLogo(null)
            setTitle("AgentGate")
        })
    }, 500)
}

export default LoginPage