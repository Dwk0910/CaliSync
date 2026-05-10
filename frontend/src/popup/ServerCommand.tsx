import { useState, useEffect } from "react";
import { IoClose } from "react-icons/io5";

import axios from "axios";
import { fetchEventSource } from "@microsoft/fetch-event-source";

import { clsx } from "clsx";

// Props for available server commands
type ServerCommandProps = {
    currentYear: number;
    backend: string;
};

export default function ServerCommand({
    servurl,
    protoSecured,
    close,

    currentYear,
    backend
}: {
    servurl: string;
    protoSecured: boolean;
    close: () => void;
} & ServerCommandProps) {
    const [log, setLog] = useState<Array<String>>([]);
    const [buttonLoading, setButtonLoading] = useState<{
        [btnName: string]: { isLoading: boolean };
    }>({ reloadSpecialDayInfo: { isLoading: false }, logout: { isLoading: false } });

    useEffect(() => {
        void fetchEventSource(
            (protoSecured ? "https://" : "http://") + servurl + "/webservice/serverlog",
            {
                headers: {
                    Authorization: localStorage.getItem("calisync_token")!.toString()
                },
                onmessage: (ev) => {
                    try {
                        setLog(JSON.parse(JSON.parse(ev.data)["body"]));
                    } catch (_) {}
                },
                openWhenHidden: true
            }
        );
    }, []);

    return (
        <div className={"w-full h-full text-white flex flex-col"}>
            <div className={"absolute w-15 h-15 right-0 text-[2.5rem] text-white"}>
                <IoClose onPointerUp={() => close()} />
            </div>
            <span className={"text-3xl font-suite font-bold"}>원격 명령</span>
            <div className={"flex flex-col w-full h-full"}>
                <span className={"font-suite text-gray-400"}>
                    CaliSync 서버에 명령을 전송합니다
                </span>
                <span className={"font-suite text-gray-400 mt-5"}>서버 로그</span>
                <div
                    className={
                        "p-2 mt-2 w-full h-[50%] bg-gray-700 rounded-lg border border-neutral-900 font-mono text-[0.75rem] overflow-scroll text-nowrap"
                    }
                >
                    {log.length > 0
                        ? log.map((line, idx) => (
                              <span key={idx}>
                                  {line}
                                  <br />
                              </span>
                          ))
                        : "로딩 중입니다..."}
                </div>
                <div
                    className={"w-full h-35 px-3 overflow-y-scroll mt-4 flex flex-col items-center"}
                >
                    <button
                        className={clsx(
                            "w-full h-10 rounded-lg mt-3 font-suite flex items-center justify-center",
                            "transition-[colors, scale] duration-200 ease-in-out active:scale-105",
                            buttonLoading["reloadSpecialDayInfo"].isLoading
                                ? "bg-gray-600"
                                : "bg-blue-500"
                        )}
                        onClick={() => {
                            setButtonLoading((prev) => ({
                                ...prev,
                                reloadSpecialDayInfo: { isLoading: true }
                            }));

                            const endLoadingAction = () => {
                                setButtonLoading((prev) => ({
                                    ...prev,
                                    reloadSpecialDayInfo: { isLoading: false }
                                }));
                            };

                            axios
                                .get(
                                    backend +
                                        `/webservice/servercmd/refreshspecialdays/${currentYear}`,
                                    {
                                        headers: {
                                            Authorization: localStorage
                                                .getItem("calisync_token")!
                                                .toString()
                                        }
                                    }
                                )
                                .then((_) => {
                                    endLoadingAction();
                                    close();
                                    window.location.reload();
                                })
                                .catch((e) => {
                                    endLoadingAction();
                                    console.error(
                                        "Error occured while refreshing special day info",
                                        e
                                    );
                                });
                        }}
                    >
                        {buttonLoading["reloadSpecialDayInfo"].isLoading ? (
                            <div
                                className={
                                    "w-5 h-5 border-3 border-t-3 border-gray-400 border-t-gray-700 rounded-full animate-spin"
                                }
                            ></div>
                        ) : (
                            <span>({currentYear}년) 기념일 정보 다시 불러오기</span>
                        )}
                    </button>
                    <button
                        className={clsx(
                            "w-full h-10 rounded-lg bg-red-400 mt-3 font-suite flex items-center justify-center",
                            "transition-[colors, scale] duration-200 ease-in-out active:scale-105"
                        )}
                        onClick={() => {
                            setButtonLoading((prev) => ({ ...prev, logout: { isLoading: true } }));
                            axios
                                .get(backend + "/webservice/servercmd/logout", {
                                    headers: {
                                        Authorization: localStorage
                                            .getItem("calisync_token")!
                                            .toString()
                                    }
                                })
                                .then((_) => {
                                    close();
                                    window.location.reload();
                                })
                                .catch((e) => {
                                    console.error(e);
                                    setButtonLoading((prev) => ({
                                        ...prev,
                                        logout: { isLoading: false }
                                    }));
                                });
                        }}
                    >
                        <span>로그아웃</span>
                    </button>
                </div>
            </div>
        </div>
    );
}
