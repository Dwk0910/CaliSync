import * as React from "react";
import { useState } from 'react';

import { Lunar } from 'lunar-javascript';
import { clsx } from 'clsx';

import { FaPlus, FaArrowLeft, FaArrowRight } from "react-icons/fa6";
import { IoTerminalOutline, IoClose } from "react-icons/io5";
import { MdMyLocation } from "react-icons/md";

// popups
import MoveTo from "./popup/MoveTo";

export default function App() {
    const [showingCalendar, setShowingCalendar] = useState<Date>(new Date());
    const [currentDay, setCurrentDay] = useState<number>(0);

    // popup 설정
    const [popup, setPopup] = useState<{open: boolean, content: React.ReactNode}>({
        open: false,
        content: <></>
    });

    const now = new Date();

    const lunar = Lunar.fromDate(showingCalendar);

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

    // 토요일 일요일 구분
    let sat = (7 - dateInfo.startDay);
    let sun = (8 - dateInfo.startDay);
    const satList = [], sunList = [];

    if (sun == 8) sunList.push(1);

    for (; sat <= dateInfo.dayCount; sat += 7) { satList.push(sat); }
    for (; sun <= dateInfo.dayCount; sun += 7) { sunList.push(sun); }

    const calendarContent: React.ReactNode[][] = [];
    let cd = 0, rd = 1, w = 0;
    for (; rd <= dateInfo.dayCount; cd++) {
        if (!calendarContent[w]) calendarContent[w] = [];

        // Add day component
        if (cd < dateInfo.startDay) {
            calendarContent[w].push(
                <div key={`empty-${cd}`} className={"flex-1 border-b border-neutral-700 h-20 p-1 pt-2"}></div>
            );
        } else {
            calendarContent[w].push(
                <div key={cd} className={clsx(
                    "flex-1 text-center border-b border-neutral-700 h-20 p-1 pt-2",
                    now.toDateString() === new Date(showingCalendar.getFullYear(), showingCalendar.getMonth(), rd).toDateString() && "bg-blue-300/20",
                    satList.includes(rd) && "text-blue-500",
                    sunList.includes(rd) && "text-red-500"
                )} onClick={() => setCurrentDay(rd)}>{rd}</div>
            );
            rd++;
        }

        if (calendarContent[w].length === 7) w++;
    }

    // 마지막 주 일수가 부족할 경우 추가
    if (calendarContent[w] && calendarContent[w].length > 0) {
        rd = 1;
        while (calendarContent[w].length < 7) {
            calendarContent[w].push(
                <div key={`fill-${showingCalendar}-${rd}`} className={"flex-1 border-b border-neutral-700 h-20 p-1 pt-2 text-center text-neutral-700"} onClick={() => {
                    setShowingCalendar((prev) => {
                        const nextDate = new Date(prev);
                        nextDate.setMonth(prev.getMonth() + 1);
                        return nextDate;
                    });
                    setCurrentDay(rd);
                }}>{rd}</div>
            );
            rd++;
        }
    }

    return (
        <>
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
                    popup.open ? "mb-0" : "-mb-[100%]"
                )} onClick={(event) => event.stopPropagation()}>
                    <div className={"absolute w-15 h-15 right-0 text-[2.5rem] text-white"}>
                        <IoClose onClick={() => setPopup((prev) => ({...prev, open: false}))}/>
                    </div>
                    { popup.content }
                </div>
            </div>
            <div className={"w-screen h-screen bg-neutral-800 text-white"}>
                <div className={"flex items-center w-full bg-neutral-700"}>
                    <div className={"w-40 h-10 flex items-center justify-center cursor-pointer"} onClick={() => window.location.assign(".")}>
                        <span className={"font-suite"}>Desktop Calendar</span>
                    </div>
                    <MdMyLocation onClick={() => setShowingCalendar(now)}/>
                    <IoTerminalOutline className={"ml-5"}/>
                    <FaArrowRight className={"ml-5"} onClick={(() => {
                        setPopup({open: true, content: <MoveTo date={ showingCalendar } setDate={ setShowingCalendar } close={() => { setPopup((prev) => ({...prev, open: false}))}}/>})
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
                <div className={"flex flex-col mt-10 font-suite"}>
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
