import * as React from "react";
import axios from "axios";
import { GoogleOAuthProvider } from '@react-oauth/google';
import { useEffect, useState, useMemo, useRef, useCallback } from "react";

import { clsx } from "clsx";

import { FaArrowLeft, FaArrowRight } from "react-icons/fa6";
import { IoTerminalOutline } from "react-icons/io5";
import { MdMyLocation } from "react-icons/md";
import { CiWarning } from "react-icons/ci";
import { RxReload } from "react-icons/rx";
import { GrUpdate } from "react-icons/gr";

// popups
import MoveTo from "./popup/MoveTo";
import EditScheulde from "./popup/EditSchedule";

// components
import LoginButtion from "./components/LoginButton";

// Type definition

// Schedules & Events
export type SpecialDay = {
    name: string;
    type: string;
    date: string;
};

export type Schedule = {
    id: string;
    date: string;
    content: string;
    isCompleted: boolean;
};

export type Day = {
    date: string;
    bgColor: string;
    mdate: string;
    schedules: Array<Schedule>;
};

export default function App() {
    // ** DIFITIONS **

    const servurl = import.meta.env.VITE_API_BACKEND_ADDRESS;
    const protoSecured = import.meta.env.VITE_API_BACKEND_PROTOCOL != "ns";
    const backend = (protoSecured ? "https://" : "http://") + servurl;

    const [now] = useState<Date>(() => new Date());

    const [sessionId, setSessionId] = useState<string>("");

    const [showingCalendar, setShowingCalendar] = useState<Date>(now);
    const [currentDay, setCurrentDay] = useState<number>(0);

    // 정상 동작 관여
    const [authorized, setAuthorized] = useState<boolean | undefined>();
    const [socketError, setSocketError] = useState<boolean>(false);
    const [loadingError, setLoadingError] = useState<boolean>(false);
    const [loadingSchedules, setLoadingSchedules] = useState<boolean>(true);

    const [specialDays, setSpecialDays] = useState<
        Map<string, Array<SpecialDay>>
    >(new Map<string, Array<SpecialDay>>());
    const [days, setDays] = useState<Map<string, Day>>(new Map<string, Day>());

    const monthInfo = useMemo(
        () => ({
            currentYear: showingCalendar.getFullYear(),
            currentMonth: showingCalendar.getMonth() + 1,
            currentDay: showingCalendar.getDate(),
            startDay: new Date(
                showingCalendar.getFullYear(),
                showingCalendar.getMonth(),
                1,
            ).getDay(),
            dayCount: new Date(
                showingCalendar.getFullYear(),
                showingCalendar.getMonth() + 1,
                0,
            ).getDate(),
        }),
        [showingCalendar],
    );

    const holidayInf = useMemo(() => {
        if (currentDay === 0) return {isHoliday: false, specdays: []};

        const specials = specialDays.get(currentDay.toString()) || [];
        const isHoliday = specials.some(
            (s) => s.type === "holi" || s.type === "rest",
        );

        return {
            isHoliday: isHoliday,
            specdays: specials,
        };
    }, [currentDay, specialDays]);

    // getDay (요일 구하기)
    const getDay: (date: Date, isHoliday: boolean) => React.ReactNode = (
        date,
        isHoliday,
    ) => {
        const style: (idx: number) => string = (idx) => {
            if (isHoliday) return "text-red-400";
            switch (idx) {
                case 0:
                    return "text-red-400";
                case 6:
                    return "text-blue-500";
                default:
                    return "text-white";
            }
        };

        const days = [
            "일요일",
            "월요일",
            "화요일",
            "수요일",
            "목요일",
            "금요일",
            "토요일",
        ];
        return (
            <span className={"font-suite " + style(date.getDay())}>
        {days[date.getDay()]}
      </span>
        );
    };

    // 1. 서버 데이터 가져오기 및 Map 생성 최적화
    const fetchSchedules = useCallback(async () => {
        setSpecialDays(new Map());
        setDays(new Map());
        setLoadingSchedules(true);
        setLoadingError(false);

        try {
            const response = await axios.get(
                backend +
                `/webservice/getMonthInfo/${showingCalendar.getFullYear()}/${showingCalendar.getMonth() + 1}`,
                {
                    headers: {
                        'X-Client-Token': localStorage.getItem("calisync_token") || ""
                    }
                }
            ).catch();

            // key: specialDays, schedules
            const responseData =
                typeof response.data === "string"
                    ? JSON.parse(response.data)
                    : response.data;

            // function to parse data
            const groupByDay = <T extends { date: string }>(data: Array<T>) => {
                const map = new Map<string, Array<T>>();
                data?.forEach((item) => {
                    const dayKey = parseInt(item.date.slice(-2)).toString();
                    if (!map.has(dayKey)) map.set(dayKey, []);
                    map.get(dayKey)!.push(item);
                });
                return map;
            };

            const {specialDays: specialDaysData, schedules: schedulesData} =
                responseData;
            // Speical Days

            if (!specialDaysData || specialDaysData.length === 0) {
                // noinspection ExceptionCaughtLocallyJS
                throw new Error("No data received");
            }
            setSpecialDays(groupByDay(specialDaysData));

            // Days (Schedules)

            const days_: Map<string, Day> = new Map<string, Day>();

            schedulesData.forEach((item: Day) => {
                const daystr = item.date.slice(-2);
                days_.set(daystr.startsWith("0") ? daystr.slice(-1) : daystr, item);
            });

            setDays(days_);

            setLoadingSchedules(false); // 로딩 종료!
        } catch (e) {
            console.error(e);

            setLoadingError(true);
            setLoadingSchedules(false);
        }
    }, [backend, showingCalendar]);

    // fetchSchedules는 showingCalendar가 바뀔때마다 수시로 바뀌므로 간접적으로 접근해야 페이지 전환마다 인증과 웹소켓 연결이 반복되지 않음
    const fetchRef = useRef(fetchSchedules);
    useEffect(() => {
        fetchRef.current = fetchSchedules;
        void fetchSchedules();
    }, [fetchSchedules]);

    useEffect(() => {
        // 클라이언트 인증
        const token = localStorage.getItem("calisync_token");
        if (token) {
            axios.post(backend + "/auth/verify", { token }).then((e) => {
                if (e.data) setAuthorized(true);
                else setAuthorized(false);
            }).catch((err) => console.error(err));
        } else setAuthorized(false);

        // 서버 이벤트 구독
        const eventSource = new EventSource(backend + "/webservice/autoRefereshEventSource");
        eventSource.onopen = () => {
            if (socketError) {
                axios.post(backend + "/auth/verify", { token }).then((e) => {
                    if (e.data) setAuthorized(true);
                    else setAuthorized(false);

                    if (sessionId) setSocketError(false);
                }).catch((err) => console.error(err));
            }
        }

        eventSource.onerror = () => {
            setSocketError(true);
        }

        eventSource.onmessage = (msg) => {
            try {
                const data = JSON.parse(msg.data);
                switch (data["code"]) {
                    case 0:
                        setSessionId(data["body"]);
                        setSocketError(false);
                        break;
                    case 600:
                        (async () => {
                            await fetchRef.current();
                        })();
                        break;
                }
            } catch (_) {
                console.warn("The server responded but received message is not in expected format.");
                console.warn(`message : ${msg.data}`);
            }
        }

        // // 웹소켓 통신 시도 및 id 등록
        // let socket_: WebSocket;
        //
        // const socketConnect = () => {
        //     socket_ = new WebSocket((protoSecured ? "wss://" : "ws://") + servurl + "/caliweb");
        //
        //     socket_.onopen = () => {
        //     }
        //
        //     socket_.onmessage = (e) => {
        //         const data = JSON.parse(e.data);
        //         switch (data["code"]) {
        //             case 0:
        //                 setSessionId(data["body"]);
        //                 break;
        //             case 600:
        //                 (async () => {
        //                     await fetchRef.current();
        //                 })();
        //                 break;
        //         }
        //     };
        //
        //     socket_.onerror = (err) => {
        //         // 에러 로깅
        //         setSocketError(true);
        //         console.error(err);
        //     }
        //
        //     socket_.onclose = async () => {
        //         // 재귀호출로 소켓 재연결 시도
        //         console.log("WebSocket closed. Reconnecting in 3s...")
        //
        //         await new Promise(resolve => setTimeout(resolve, 3000));
        //         socketConnect();
        //     }
        //
        //     socket.current = socket_;
        // }

        // // 모바일용 웹소켓 재연결 로직
        // const handleFocus = () => {
        //     if (document.visibilityState === "visible" && socket.current?.readyState === WebSocket.CLOSED) socketConnect();
        // };
        //
        // document.addEventListener("visibilitychange", handleFocus);
        //
        // // 최초 웹소켓 연결
        // socketConnect();
        //
        // return () => {
        //     document.removeEventListener("visibilitychange", handleFocus);
        //
        //     if (socket.current) {
        //         // 재연결 방지
        //         socket.current.onclose = null;
        //         socket.current.close();
        //     }
        // }
    }, [backend, protoSecured, servurl]);

    // 백엔드 서버 통신 시도 및 국가 이벤트, 사용자 이벤트 호출하기

    // 2. 달력 컨텐츠 생성
    const calendarContent: React.ReactNode[][] = useMemo(() => {
        const calendarContent_: React.ReactNode[][] = [];
        const now = new Date();

        let cd = 0,
            rd = 1,
            w = 0;
        while (rd <= monthInfo.dayCount) {
            if (!calendarContent_[w]) calendarContent_[w] = [];

            if (cd < monthInfo.startDay) {
                // 시작이 일요일이 아닌 경우
                calendarContent_[w].push(
                    <div
                        key={`empty-${cd}`}
                        className="flex-1 border-b border-neutral-700 h-20 pt-2"
                    ></div>,
                );
            } else {
                const day = rd;
                const isToday =
                    now.getFullYear() === showingCalendar.getFullYear() &&
                    now.getMonth() === showingCalendar.getMonth() &&
                    now.getDate() === day;

                // 날짜 색상 결정 로직
                const daySpecials = specialDays.get(day.toString()) || [];
                const hasHoliday = daySpecials.some(
                    (s) => s.type === "holi" || s.type === "rest",
                );
                const isSat = cd % 7 === 6;
                const isSun = cd % 7 === 0;

                // Schedule data
                // const schedule_data: Map<string, Array<Schedule>> = schedules.get(day.toString());
                // const schedules_: Array<Schedule> = schedule_data.get("schedules") || [];

                // Schedule icon
                const scheduleLength = loadingSchedules
                    ? 0
                    : days.get(day.toString())?.schedules?.length || 0;

                calendarContent_[w].push(
                    <div
                        key={`day-${day}`}
                        className={clsx(
                            "flex-1 text-center border-b border-neutral-700 h-20 pt-2 cursor-pointer",
                            isToday && "bg-blue-300/20",
                            "flex flex-col",
                        )}
                        onPointerUp={() => {
                            if (!loadingSchedules) {
                                setCurrentDay(day);
                                setPopup((prev) => ({
                                    ...prev,
                                    open: true,
                                    content: "EditSchedule",
                                }));
                            }
                        }}
                    >
                    <span
                        className={clsx(
                            isSat && "text-blue-500",
                            (isSun || hasHoliday) && "text-red-500",
                        )}
                    >
                      {day}
                    </span>
                        <div className={"w-full flex flex-wrap justify-start"}>
                            {(() => {
                                const result: React.ReactNode[] = [];
                                for (let i = 0; i < scheduleLength; i++) {
                                    // 7개 이상부터는 표시X
                                    if (i == 7) break;
                                    result.push(
                                        <div
                                            key={`scheduleItem-${i}`}
                                            className={"mt-1 w-3 h-3 px-1.5"}
                                        >
                                            <div
                                                className={"w-2 h-2 rounded-[100%] bg-gray-400/50"}
                                            />
                                        </div>,
                                    );
                                }

                                return result;
                            })()}
                        </div>
                    </div>,
                );
                rd++;
            }
            cd++;
            if (calendarContent_[w].length === 7) w++;
        }

        // 마지막 주 일수가 부족할 경우 추가 (Clickable)
        // if (calendarContent_[w] && calendarContent_[w].length > 0) {
        //     rd = 1;
        //     while (calendarContent_[w].length < 7) {
        //         calendarContent_[w].push(
        //             <div key={`fill-${showingCalendar}-${rd}`} className={"flex-1 border-b border-neutral-700 h-20 p-1 pt-2 text-center text-neutral-700"} onPointerUp={() => {
        //                 setShowingCalendar((prev) => {
        //                     const nextDate = new Date(prev);
        //                     nextDate.setMonth(prev.getMonth() + 1);
        //                     return nextDate;
        //                 });
        //                 setCurrentDay(rd);
        //             }}>{rd}</div>
        //         );
        //         rd++;
        //     }
        // }

        // (Non-Clickable)
        if (calendarContent_[w] && calendarContent_[w].length > 0) {
            rd = 1;
            while (calendarContent_[w].length < 7) {
                calendarContent_[w].push(
                    <div
                        key={`fill-${showingCalendar}-${rd}`}
                        className={
                            "flex-1 border-b border-neutral-700 h-20 pt-2 text-center text-neutral-700"
                        }
                    />,
                );
                rd++;
            }
        }

        return calendarContent_;
    }, [loadingSchedules, monthInfo, showingCalendar, specialDays, days]); // specialDays를 의존성에 넣어야 로딩 후 빨간색이 칠해짐!

    // popup 설정
    // close function for Popup classes
    const close = () => {
        setPopup((prev) => ({...prev, open: false}));
    };

    // ** DEFINE POPUPS HERE **
    const popups: {
        [key: string]: {
            component: React.ReactNode;
            allowBgClose: boolean;
            height: string;
        };
    } = {
        EditSchedule: {
            component: (
                <EditScheulde
                    now={now}
                    showingCalendar={showingCalendar}
                    day={
                        days.get(currentDay.toString()) || {
                            date: `${showingCalendar.getFullYear()}${monthInfo.currentMonth.toString().length == 1 ? "0" + monthInfo.currentMonth : monthInfo.currentMonth}${currentDay.toString().length == 1 ? "0" + currentDay : currentDay.toString()}`,
                            bgColor: "",
                            mdate: "",
                            schedules: [],
                        }
                    }
                    holidayInf={holidayInf}
                    getDayName={getDay}
                    refresh={fetchSchedules}
                    close={close}
                    setAllowBgClose={(allow: boolean) => {
                        setPopup((prev) => {
                            if (prev.content == "EditSchedule")
                                return {...prev, allowBgClose: allow};
                            else return prev;
                        });
                    }}
                    sessionId={sessionId}
                    backend={backend}
                />
            ),
            // 변경점이 있거나 로딩중일땐 배경눌러 팝업닫기를 차단하고, 그 외의 상황에서는 풀기
            allowBgClose: false,
            height: "90%",
        },
        MoveTo: {
            component: (
                <MoveTo
                    date={showingCalendar}
                    setDate={setShowingCalendar}
                    close={close}
                />
            ),
            allowBgClose: true,
            height: "350px",
        },
    };

    // popup state
    const [popup, setPopup] = useState<{
        open: boolean;
        content: string;
        allowBgClose: boolean;
    }>({
        open: false,
        content: "",
        allowBgClose: true,
    });

    // 비인증 시 단순히 가리는 게 아니라 아예 출력 자체가 안되어야 하므로 return문을 따로 작성

    if (authorized == undefined || socketError) {
        return (
            <div className={"w-full h-screen flex items-center justify-center bg-neutral-700"}>
                <span className={"font-suite text-white"}>{!socketError ? "클라이언트 인증 중입니다..." : (<div className={"text-center"}>서버와 통신하는 중 오류가 발생했습니다<br/><span className={"text-gray-400"}>서버 응답 수신 시 새로고침됩니다.</span></div>)}</span>
            </div>
        );
    } else if (!authorized) {
        return (
            <div className={"w-full h-screen flex flex-col items-center justify-center bg-neutral-700"}>
                <span className={"font-suite text-white mb-1"}>
                    <span className={"text-gray-500"}>HTTP</span>
                    <span className={"text-red-400"}>401</span>
                    <span className={"mx-2"}>:</span>
                    <span className={"font-bold"}>Unauthorized</span>
                </span>
                <GoogleOAuthProvider clientId={import.meta.env.VITE_API_GOOGLEOAUTH_CLIENT_ID}>
                    <LoginButtion backend={ backend } setAuthorized={ setAuthorized } fetchSchedules={ fetchSchedules }/>
                </GoogleOAuthProvider>
            </div>
        );
    }

    // 정상 출력
    return (
        <>
            {
                // 스크린 블로킹 버전 Loading screen
                // loadingSchedules && (
                //     <div className={"fixed w-screen h-screen flex justify-center items-center bg-black/70"}>
                //         <div className={"flex flex-col p-5 w-70 h-25 bg-neutral-700 rounded-lg"}>
                //             <span className={"font-suite text-[1.2rem] text-white"}>로딩 중</span>
                //             <span className={"font-suite text-[0.9rem] text-gray-400"}>서버와 통신 중입니다...</span>
                //         </div>
                //     </div>
                // )
            }

            {/* Popup */}
            {(() => {
                // Default popup height : 500px
                const height = popups[popup.content]?.height ?? "500px";
                const popupConfig = popups[popup.content];
                return (
                    <div
                        className={clsx(
                            "fixed w-screen h-screen",
                            "flex flex-col justify-end",
                            "transition-colors duration-200 ease-in-out",
                            popup.open && "bg-black/70",
                            !popup.open && "pointer-events-none",
                        )}
                        onPointerDown={() =>
                            setPopup((prev) => {
                                return popup.allowBgClose ? {...prev, open: false} : prev;
                            })
                        }
                    >
                        <div
                            style={{
                                height,
                                bottom: popup.open ? "0" : `-${height}`
                            }}
                            className={clsx(
                                "fixed w-screen bg-neutral-700",
                                "transition-all duration-200 ease-in-out",
                                "pt-6 px-5",
                            )}
                            onPointerDown={(event) => event.stopPropagation()}
                        >
                            {popupConfig?.component}
                        </div>
                    </div>
                );
            })()}

            {/*Calendar*/}
            <style>{`body { background-color: #262626; }`}</style>
            <div className={"w-screen h-screen text-white"}>
                <div className={"flex items-center w-full bg-neutral-700"}>
                    <div
                        className={
                            "w-40 h-10 flex items-center justify-center cursor-pointer"
                        }
                        onPointerUp={() => window.location.assign(".")}
                    >
                        <span className={"font-suite"}>Desktop Calendar</span>
                    </div>
                    <MdMyLocation onPointerUp={() => setShowingCalendar(now)}/>
                    <IoTerminalOutline className={"ml-5"}/>
                    <FaArrowRight
                        className={"ml-5"}
                        onPointerUp={() => {
                            setPopup((prev) => ({...prev, open: true, content: "MoveTo"}));
                        }}
                    />
                    <RxReload
                        className={"ml-5 font-bold"}
                        onPointerUp={async () => await fetchSchedules()}
                    />
                </div>
                <div
                    className={clsx(
                        "inline-block p-2 text-green-300/30 animate-spin transition-opacity duration-300 ease-in-out",
                        loadingSchedules ? "opacity-100" : "opacity-0",
                    )}
                >
                    <GrUpdate/>
                </div>
                <div className={"flex justify-center"}>
                    <div
                        className={
                            "flex mt-auto mr-10 mb-2 items-center justify-center p-2 w-10 h-10 text-[1.5rem] bg-gray-500 rounded-full"
                        }
                        onPointerUp={() => {
                            setShowingCalendar(
                                new Date(
                                    showingCalendar.getFullYear(),
                                    showingCalendar.getMonth() - 1,
                                    1,
                                ),
                            );
                        }}
                    >
                        <FaArrowLeft/>
                    </div>
                    <div
                        className={
                            "flex flex-col justify-end font-suite items-center h-15 mt-5"
                        }
                    >
                        <span className={"text-gray-300 w-20 text-center"}>
                          {monthInfo.currentYear}년
                        </span>
                                    <span className={"-mt-1 text-[2rem] font-bold w-20 text-center"}>
                          {monthInfo.currentMonth}월
                        </span>
                    </div>
                    <div
                        className={
                            "flex mt-auto ml-10 mb-2 items-center justify-center p-2 w-10 h-10 text-[1.5rem] bg-gray-500 rounded-full"
                        }
                        onPointerUp={() => {
                            setShowingCalendar(
                                new Date(
                                    showingCalendar.getFullYear(),
                                    showingCalendar.getMonth() + 1,
                                    1,
                                ),
                            );
                        }}
                    >
                        <FaArrowRight/>
                    </div>
                </div>
                <div className={"w-full flex justify-center"}>
                    <div className={"flex flex-col mt-5 font-suite w-full max-w-[320px]"}>
                        <div
                            className={clsx(
                                "mb-1 flex items-center",
                                "text-[1.1rem]",
                                "transition-all duration-200",
                                loadingError ? "opacity-100 -mt-2" : "opacity-0 -mt-7",
                            )}
                        >
                        <span
                            className={"ml-4 text-[1.2rem] mb-0.5 text-red-400 font-bold"}
                        >
                          <CiWarning/>
                        </span>
                            <span className={"ml-2 text-red-400"}>기념일 데이터 불러오기 실패</span>
                        </div>
                        <div
                            className={
                                "flex w-full border-b border-gray-400 pb-2 font-bold text-[1.2rem]"
                            }
                        >
                            <span className={"flex-1 text-center text-red-400"}>일</span>
                            <span className={"flex-1 text-center"}>월</span>
                            <span className={"flex-1 text-center"}>화</span>
                            <span className={"flex-1 text-center"}>수</span>
                            <span className={"flex-1 text-center"}>목</span>
                            <span className={"flex-1 text-center"}>금</span>
                            <span className={"flex-1 text-center text-blue-600"}>토</span>
                        </div>
                        {calendarContent.map((item, idx) => {
                            return (
                                <div key={idx} className={"flex font-suite"}>
                                    {item}
                                </div>
                            );
                        })}
                    </div>
                </div>
            </div>
        </>
    );
}
