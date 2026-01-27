import * as React from "react";
import $ from 'jquery';
import { useEffect, useMemo, useState } from 'react';

import { Lunar } from 'lunar-javascript';
import { clsx } from 'clsx';

import { FaPlus, FaArrowLeft, FaArrowRight } from "react-icons/fa6";
import { IoTerminalOutline, IoClose } from "react-icons/io5";
import { GrUpdate } from "react-icons/gr";
import { CiWarning } from 'react-icons/ci'
import { MdMyLocation } from "react-icons/md";

// popups
import MoveTo from "./popup/MoveTo";

export default function App() {
    const backend = import.meta.env.VITE_API_BACKEND_ADDRESS;

    const [showingCalendar, setShowingCalendar] = useState<Date>(new Date());
    const [currentDay, setCurrentDay] = useState<number>(0);
    const [scheduleOpen, setScheduleOpen] = useState<boolean>(false);

    const [loadingError, setLoadingError] = useState<boolean>(false);
    const [loadingSchedules, setLoadingSchedules] = useState<boolean>(true);
    const [specialDays, setSpecialDays] = useState<Map<string, Array<SpecialDay>>>(new Map<string, Array<SpecialDay>>());

    const [now] = useState<Date>(() => new Date());

    // 1. 서버 데이터 가져오기 및 Map 생성 최적화
    useEffect(() => {
        let isMounted = true;

        const fetchSchedules = async () => {
            setSpecialDays(new Map());
            setLoadingSchedules(true);
            setLoadingError(false);

            try {
                const response = await $.ajax({
                    url: backend + `/webservice/specialdays/${showingCalendar.getFullYear()}/${showingCalendar.getMonth() + 1}`,
                    method: "GET",
                });

                if (!isMounted) return;

                const data_: Array<SpecialDay> = typeof response === 'string' ? JSON.parse(response) : response;
                const specialDays_: Map<string, Array<SpecialDay>> = new Map();

                if (data_ && data_.length > 0) {
                    data_.forEach(item => {
                        const dayKey = parseInt(item.date.slice(-2)).toString();
                        if (!specialDays_.has(dayKey)) specialDays_.set(dayKey, []);
                        specialDays_.get(dayKey)!.push(item);
                    });
                } else {
                    // noinspection ExceptionCaughtLocallyJS
                    throw new Error("No data received");
                }


                setSpecialDays(specialDays_);
                setLoadingSchedules(false); // 로딩 종료!
            } catch (e) {
                console.error(e);

                if (!isMounted) return;
                setLoadingError(true);
                setLoadingSchedules(false);
            }
        };

        void fetchSchedules();

        return () => { isMounted = false; }; // 언마운트 시 클린업
    }, [backend, showingCalendar]);

    // getDay (요일 구하기)
    const getDay: (date: Date, isHoliday: boolean) => React.ReactNode = (date, isHoliday) => {
        const style: (idx: number) => string = (idx) => {
            if (isHoliday) return "text-red-400";
            switch (idx) {
                case 0: return "text-red-400";
                case 6: return "text-blue-500";
                default: return "text-white";
            }
        }

        const days = ["일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일"];
        return (<span className={"font-suite " + style(date.getDay())}>{days[date.getDay()]}</span>);
    }

    // popup 설정
    const [popup, setPopup] = useState<{open: boolean, content: React.ReactNode}>({
        open: false,
        content: <></>
    });

    const monthInfo = useMemo(() => ({
        currentYear: showingCalendar.getFullYear(),
        currentMonth: showingCalendar.getMonth() + 1,
        currentDay: showingCalendar.getDate(),
        startDay: new Date(showingCalendar.getFullYear(), showingCalendar.getMonth(), 1).getDay(),
        dayCount: new Date(showingCalendar.getFullYear(), showingCalendar.getMonth() + 1, 0).getDate()
    }), [showingCalendar]);

    const currentDayInf = useMemo(() => {
        if (currentDay === 0) return { isHoliday: false, specdays: [] };

        const specials = specialDays.get(currentDay.toString()) || [];
        const isHoliday = specials.some(s => s.type === "holi" || s.type === "rest");

        return {
            isHoliday: isHoliday,
            specdays: specials
        };
    }, [currentDay, specialDays]);

    // close function for Popup classes
    const close = () => {
        setPopup((prev) => ({...prev, open: false}));
    }

    // 백엔드 서버 통신 시도 및 국가 이벤트, 사용자 이벤트 호출하기

    // Schedules & Events
    type SpecialDay = {
        name: string;
        type: string;
        date: string;
    }

    // 2. 달력 컨텐츠 생성
    const calendarContent: React.ReactNode[][] = useMemo(() => {
        const calendarContent_: React.ReactNode[][] = [];
        const now = new Date();

        let cd = 0, rd = 1, w = 0;
        while (rd <= monthInfo.dayCount) {
            if (!calendarContent_[w]) calendarContent_[w] = [];

            if (cd < monthInfo.startDay) {
                calendarContent_[w].push(<div key={`empty-${cd}`} className="flex-1 border-b border-neutral-700 h-20 p-1 pt-2"></div>);
            } else {
                const day = rd;
                const isToday = now.getFullYear() === showingCalendar.getFullYear() &&
                    now.getMonth() === showingCalendar.getMonth() &&
                    now.getDate() === day;

                // 날짜 색상 결정 로직
                const daySpecials = specialDays.get(day.toString()) || [];
                const hasHoliday = daySpecials.some(s => s.type === "holi" || s.type === "rest");
                const isSat = (cd % 7 === 6);
                const isSun = (cd % 7 === 0);

                calendarContent_[w].push(
                    <div key={`day-${day}`}
                         className={clsx(
                             "flex-1 text-center border-b border-neutral-700 h-20 p-1 pt-2 cursor-pointer",
                             isToday && "bg-blue-300/20",
                             isSat && "text-blue-500",
                             (isSun || hasHoliday) && "text-red-500"
                         )}
                         onClick={() => {
                             if (!loadingSchedules) {
                                 setCurrentDay(day);
                                 setScheduleOpen(true);
                             }
                         }}>
                        {day}
                    </div>
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
        //             <div key={`fill-${showingCalendar}-${rd}`} className={"flex-1 border-b border-neutral-700 h-20 p-1 pt-2 text-center text-neutral-700"} onClick={() => {
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
                    <div key={`fill-${showingCalendar}-${rd}`} className={"flex-1 border-b border-neutral-700 h-20 p-1 pt-2 text-center text-neutral-700"}/>
                );
                rd++;
            }
        }

        return calendarContent_;
    }, [loadingSchedules, monthInfo, showingCalendar, specialDays]); // specialDays를 의존성에 넣어야 로딩 후 빨간색이 칠해짐!



    const [daypopup_button_active, set_daypopup_button_active] = useState<boolean>(true);

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

            {/*General Popup*/}
            <div className={clsx(
                "fixed w-screen h-screen",
                "flex flex-col justify-end",
                "transition-colors duration-200 ease-in-out",
                popup.open && "bg-black/70",
                !popup.open && "pointer-events-none"
            )} onClick={() => setPopup((prev) => ({...prev, open: false}))}>
                <div className={clsx(
                    "fixed w-screen h-[50%] bg-neutral-700",
                    "transition-all duration-200 ease-in-out",
                    "pt-6 px-6 relative",
                    popup.open ? "mb-0" : "-mb-[100vh]"
                )} onClick={(event) => event.stopPropagation()}>
                    <div className={"absolute w-15 h-15 right-0 text-[2.5rem] text-white"}>
                        <IoClose onClick={() => setPopup((prev) => ({...prev, open: false}))}/>
                    </div>
                    { popup.content }
                </div>
            </div>

            {/*Schedule Setting Popup (Day popup)*/}
            <div className={clsx(
                "fixed w-screen h-screen",
                "flex flex-col justify-end",
                "transition-colors duration-200 ease-in-out",
                scheduleOpen && "bg-black/70",
                !scheduleOpen && "pointer-events-none"
            )} onClick={() => setScheduleOpen(false)}>
                <div className={clsx(
                    "fixed w-screen h-150 bg-neutral-700",
                    "transition-all duration-300 ease-in-out",
                    "pt-6 px-6 relative",
                    "flex flex-col justify-between",
                    scheduleOpen ? "mb-0" : "-mb-150"
                )} onClick={(event) => event.stopPropagation()}>
                    <div className={"flex flex-col font-suite text-white"}>
                        <span className={"mx-auto mb-3 text-white text-[1rem]"}>일정 수정</span>
                        <span className={"text-[1.5rem] text-gray-300"}>{ showingCalendar.getFullYear() }년</span>
                        <div>
                            <span className={"text-[2rem]"}>
                                <span>{ showingCalendar.getMonth() + 1 }월</span>
                                <span className={"ml-2"}>{ currentDay }일</span>
                            </span>
                            {(() => {
                                const date = new Date(showingCalendar.getFullYear(), showingCalendar.getMonth(), currentDay);
                                const lunar = Lunar.fromDate(date);
                                return (
                                    <span className={"ml-3 text-gray-400"}>{ getDay(date, currentDayInf.isHoliday) }<span className={"mx-2"}>·</span>(음) { lunar.getMonth() }월 { lunar.getDay() }일</span>
                                )
                            })()}
                        </div>
                        <div className={"flex gap-2 flex-wrap mt-1"}>
                            {now.getFullYear() == showingCalendar.getFullYear() && now.getMonth() == showingCalendar.getMonth() && now.getDate() == currentDay && (
                                <div className={"inline-block px-2 h-5 text[0.9rem] rounded-[5px] bg-blue-900 text-white font-bold"}>
                                    오늘
                                </div>
                            )}
                            {currentDayInf.specdays.map((i, idx) => {
                                return (
                                    <div key={`specialday-${idx}`} className={
                                        clsx(
                                        "inline-block px-2 h-5 text-[0.9rem] rounded-[5px]",
                                            i.type === "holi" && "bg-red-700 text-red-100 font-bold",
                                            i.type === "rest" && "bg-red-500 font-bold",
                                            i.type === "anni" && "bg-purple-400 text-black",
                                            i.type === "tfst" && "bg-[#F9A825]",
                                            i.type === "other" && "bg-gray-400 text-black"
                                        )
                                    }>{i.name}</div>
                                );
                            })}
                        </div>
                    </div>
                    <div className={"flex"}>
                        <div className={clsx(
                            "flex justify-center items-center w-[50%] h-12 rounded-lg",
                            "transition-all duration-200 ease-in-out mb-10 border border-gray-600",
                            "bg-neutral-500"
                        )} onClick={() => {
                            setScheduleOpen(false);
                        }}>
                            <span className={"font-suite text-xl"}>취소</span>
                        </div>
                        <div className={clsx(
                            "flex justify-center items-center ml-5 w-[50%] h-12 rounded-lg",
                            "transition-all duration-200 ease-in-out mb-10",
                            daypopup_button_active ? "bg-green-600/90" : "bg-neutral-600")
                        } onClick={() => {
                            if (daypopup_button_active) return;
                            setScheduleOpen(false);
                        }}>
                            <span className={"font-suite text-xl"}>저장</span>
                        </div>
                    </div>
                </div>
            </div>

            {/*Calendar*/}
            <div className={"w-screen h-screen bg-neutral-800 text-white"}>
                <div className={"flex items-center w-full bg-neutral-700"}>
                    <div className={"w-40 h-10 flex items-center justify-center cursor-pointer"} onClick={() => window.location.assign(".")}>
                        <span className={"font-suite"}>Desktop Calendar</span>
                    </div>
                    <MdMyLocation onClick={() => setShowingCalendar(now)}/>
                    <IoTerminalOutline className={"ml-5"}/>
                    <FaArrowRight className={"ml-5"} onClick={(() => {
                        setPopup({open: true, content: <MoveTo date={ showingCalendar } setDate={ setShowingCalendar } close={ close }/>})
                    })}/>
                    <FaPlus className={"ml-5"}/>
                </div>
                <div className={clsx("inline-block p-2 text-green-300/30 animate-spin transition-opacity duration-300 ease-in-out", loadingSchedules ? "opacity-100" : "opacity-0")}>
                    <GrUpdate/>
                </div>
                <div className={"flex justify-center"}>
                    <div className={"flex mt-auto mr-10 mb-2 items-center justify-center p-2 w-10 h-10 text-[1.5rem] bg-gray-500 rounded-full"} onClick={() => {
                        setShowingCalendar(new Date(showingCalendar.getFullYear(), showingCalendar.getMonth() - 1, showingCalendar.getDate()));
                    }}>
                        <FaArrowLeft/>
                    </div>
                    <div className={"flex flex-col justify-end font-suite items-center h-15 mt-5"}>
                        <span className={"text-gray-300 w-20 text-center"}>{ monthInfo.currentYear }년</span>
                        <span className={"-mt-1 text-[2rem] font-bold w-20 text-center"}>{ monthInfo.currentMonth }월</span>
                    </div>
                    <div className={"flex mt-auto ml-10 mb-2 items-center justify-center p-2 w-10 h-10 text-[1.5rem] bg-gray-500 rounded-full"} onClick={() => {
                        setShowingCalendar(new Date(showingCalendar.getFullYear(), showingCalendar.getMonth() + 1, showingCalendar.getDate()));
                    }}>
                        <FaArrowRight/>
                    </div>
                </div>
                <div className={"flex flex-col mt-5 font-suite"}>
                    <div className={clsx(
                        "mb-1 flex items-center",
                        "text-[1.1rem]",
                        "transition-all duration-200",
                        loadingError ? "opacity-100 -mt-2" : "opacity-0 -mt-7"
                    )}>
                        <span className={"ml-4 text-[1.2rem] mb-0.5 text-red-400 font-bold"}><CiWarning/></span>
                        <span className={"ml-2 text-red-400"}>기념일 데이터 불러오기 실패</span>
                    </div>
                    <div className={"flex w-full border-b border-gray-400 pb-2 font-bold text-[1.2rem]"}>
                        <span className={"flex-1 text-center text-red-400"}>일</span>
                        <span className={"flex-1 text-center"}>월</span>
                        <span className={"flex-1 text-center"}>화</span>
                        <span className={"flex-1 text-center"}>수</span>
                        <span className={"flex-1 text-center"}>목</span>
                        <span className={"flex-1 text-center"}>금</span>
                        <span className={"flex-1 text-center text-blue-600"}>토</span>
                    </div>
                    {
                        calendarContent.map((item, idx) => {
                            return (
                                <div key={idx} className={"flex font-suite"}>
                                    {item}
                                </div>
                            );
                        })
                    }
                </div>
            </div>
        </>
    );
}
