import * as React from "react";
import $ from 'jquery';
import { useEffect, useMemo, useState } from 'react';

import { Lunar } from 'lunar-javascript';
import { clsx } from 'clsx';

import { FaPlus, FaArrowLeft, FaArrowRight } from "react-icons/fa6";
import { IoTerminalOutline, IoClose } from "react-icons/io5";
import { CiWarning } from 'react-icons/ci'

import { MdMyLocation } from "react-icons/md";

// popups
import MoveTo from "./popup/MoveTo";

export default function App() {
    const backend = import.meta.env.VITE_API_BACKEND_ADDRESS;

    const [showingCalendar, setShowingCalendar] = useState<Date>(new Date());
    const [currentDay, setCurrentDay] = useState<number>(0);
    const [scheduleOpen, setScheduleOpen] = useState<boolean>(false);

    const [now] = useState<Date>(() => new Date());

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

    const simplifyDateString: (year: number, month: number, day: number) => string = (year, month, day) => {
        return `${year}${(month + 1).toString().length == 1 ? ("0" + (month + 1).toString()) : (month + 1).toString()}${day.toString().length == 1 ? ("0" + day.toString()) : day.toString()}`
    }

    // popup 설정
    const [popup, setPopup] = useState<{open: boolean, content: React.ReactNode}>({
        open: false,
        content: <></>
    });

    const year = showingCalendar.getFullYear();
    const month = showingCalendar.getMonth();
    const day = showingCalendar.getDate();

    const firstDay = new Date(year, month, 1).getDay();
    const totalDays = new Date(year, month + 1, 0).getDate();

    const dateInfo = {
        currentYear: year,
        currentMonth: month + 1,
        currentDay: day,
        startDay: firstDay,
        dayCount: totalDays
    };

    // close function for Popup classes
    const close = () => {
        setPopup((prev) => ({...prev, open: false}));
    }

    const [loadingError, setLoadingError] = useState<boolean>(false);
    const [loadingSchedules, setLoadingSchedules] = useState<boolean>(true);
    const [specialDays, setSpecialDays] = useState<Array<SpecialDay>>([]);

    // 백엔드 서버 통신 시도 및 국가 이벤트, 사용자 이벤트 호출하기

    // Schedules & Events
    type SpecialDay = {
        name: string;
        type: string;
        date: string;
    }
    
    useEffect(() => {
        setLoadingSchedules(true);
        $.ajax({
            url: backend + `/webservice/specialdays/${showingCalendar.getFullYear()}/${showingCalendar.getMonth() + 1}`,
            type: "application/json",
            method: "GET",
        }).then((e: string) => {
            const data: Array<SpecialDay> = JSON.parse(e);
            if (data.length == 0) setLoadingError(true);
            else setLoadingError(false);
            setSpecialDays(data);
        })
    }, [backend, showingCalendar]);

    // 달력 만들기
    const calendarContent: React.ReactNode[][] = useMemo(() => {
        const calendarContent_: React.ReactNode[][] = [];

        const now = new Date();

        // 토요일 일요일
        let sat = (7 - dateInfo.startDay);
        let sun = (8 - dateInfo.startDay);
        const satList = [], sunList = [];

        if (sun == 8) sunList.push(1);

        for (; sat <= dateInfo.dayCount; sat += 7) { satList.push(sat); }
        for (; sun <= dateInfo.dayCount; sun += 7) { sunList.push(sun); }

        // 1. 이번 달 날짜 채우기 (빈 칸 포함)
        // cd가 startDay보다 작을 때는 빈 칸을 넣고, 그 이후부터 rd를 증가시킴
        let cd = 0, rd = 1, w = 0;
        while (rd <= dateInfo.dayCount) {
            if (!calendarContent_[w]) calendarContent_[w] = [];

            if (cd < dateInfo.startDay) {
                calendarContent_[w].push(
                    <div key={`empty-${cd}`} className="flex-1 border-b border-neutral-700 h-20 p-1 pt-2"></div>
                );
            } else {
                // 클로저 문제 방지를 위해 현재 rd 값을 상수로 고정
                const day = rd;
                calendarContent_[w].push(
                    <div key={`day-${day}`} className={clsx(
                        "flex-1 text-center border-b border-neutral-700 h-20 p-1 pt-2",
                        now.toDateString() === new Date(showingCalendar.getFullYear(), showingCalendar.getMonth(), day).toDateString() && "bg-blue-300/20",
                        satList.includes(day) && "text-blue-500",
                        (sunList.includes(day) || specialDays.find((i) => (i.date === simplifyDateString(showingCalendar.getFullYear(), showingCalendar.getMonth(), day) && ((i.type == "holi" && i.name != "제헌절") || i.type == "rest")))) && "text-red-500",
                    )} onClick={() => {
                        setCurrentDay(day);
                        setScheduleOpen(true);
                    }}>{day}</div>
                );
                rd++;
            }

            cd++; // 무한 루프 방지를 위해 cd는 항상 증가
            if (calendarContent_[w].length === 7) w++;
        }

        // 2. 마지막 주 빈 칸을 다음 달 날짜로 채우기
        if (calendarContent_[w] && calendarContent_[w].length > 0) {
            let nextMonthDay = 1;
            while (calendarContent_[w].length < 7) {
                const day = nextMonthDay;
                calendarContent_[w].push(
                    <div key={`fill-${day}`} className="flex-1 border-b border-neutral-700 h-20 p-1 pt-2 text-center text-neutral-700"
                         onClick={() => {
                             setShowingCalendar((prev) => {
                                 const nextDate = new Date(prev);
                                 nextDate.setMonth(prev.getMonth() + 1);
                                 return nextDate;
                             });
                             setCurrentDay(day);
                             setScheduleOpen(true);
                         }}>
                        {day}
                    </div>
                );
                nextMonthDay++;
            }
        }

        // 3. set loading state to false
        setLoadingSchedules(false);

        // 4. return
        return calendarContent_;
    }, [dateInfo.startDay, dateInfo.dayCount, showingCalendar, specialDays]);

    // currentDay에 해당하는 Special Day 및 공휴일 여부 구해두기
    const current_holi_info: { isHoliday: boolean, specdays: Array<SpecialDay> } = useMemo(() => {
        let haveHoliday: boolean = false;
        const specialDays_: Array<SpecialDay> = [];
        for (const item of specialDays) {
            if (item.date == simplifyDateString(showingCalendar.getFullYear(), showingCalendar.getMonth(), currentDay)) {
                specialDays_.push(item);
                if (item.type === "holi" || item.date === "rest") haveHoliday = true;
            }
        }

        return {
            isHoliday: haveHoliday,
            specdays: specialDays_,
        };
    }, [currentDay, showingCalendar, specialDays]);

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
                                    <span className={"ml-3 text-gray-400"}>{ getDay(date, current_holi_info.isHoliday) }<span className={"mx-2"}>·</span>(음) { lunar.getMonth() }월 { lunar.getDay() }일</span>
                                )
                            })()}
                        </div>
                        <div className={"flex gap-2 flex-wrap mt-1"}>
                            {now.getFullYear() == showingCalendar.getFullYear() && now.getMonth() == showingCalendar.getMonth() && now.getDate() == currentDay && (
                                <div className={"inline-block px-2 h-5 text[0.9rem] rounded-[5px] bg-blue-900 text-white font-bold"}>
                                    오늘
                                </div>
                            )}
                            {current_holi_info.specdays.map((i, idx) => {
                                return (
                                    <div key={`specialday-${idx}`} className={
                                        clsx(
                                        "inline-block px-2 h-5 text-[0.9rem] rounded-[5px]",
                                            i.type === "holi" && "bg-red-700 text-red-100 font-bold",
                                            i.type === "rest" && "bg-red-400 font-bold",
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
                <div className={"flex m-5 justify-center"}>
                    <div className={"flex mt-auto mr-10 mb-2 items-center justify-center p-2 w-10 h-10 text-[1.5rem] bg-gray-500 rounded-full"} onClick={() => {
                        setShowingCalendar(new Date(showingCalendar.getFullYear(), showingCalendar.getMonth() - 1, showingCalendar.getDate()));
                    }}>
                        <FaArrowLeft/>
                    </div>
                    <div className={"flex flex-col justify-end font-suite items-center h-15 mt-5"}>
                        <span className={"text-gray-300 w-20 text-center"}>{ dateInfo.currentYear }년</span>
                        <span className={"-mt-1 text-[2rem] font-bold w-20 text-center"}>{ dateInfo.currentMonth }월</span>
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
