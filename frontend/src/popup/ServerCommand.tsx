import { useState, useEffect } from "react";
import { IoClose } from "react-icons/io5";

import { fetchEventSource } from "@microsoft/fetch-event-source";

export default function ServerCommand({
    servurl,
    protoSecured,
    close
}: {
    servurl: string;
    protoSecured: boolean;
    close: () => void;
}) {
    const [log, setLog] = useState<Array<String>>([]);

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
            </div>
        </div>
    );
}
