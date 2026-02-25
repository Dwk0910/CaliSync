import * as React from 'react';
import { useState, useEffect } from 'react';

import no_color from '../assets/editscheudle_popup_asset/no_color.png';

import { type SpecialDay, type Schedule, type Day } from "../App";
import { type Popup } from './Popup';

import { Reorder, AnimatePresence, useDragControls, motion, useAnimation } from 'framer-motion';

import { IoAddOutline } from 'react-icons/io5';
import { RiDeleteBin6Line } from "react-icons/ri";
import { RxDragHandleDots2 } from "react-icons/rx";
import { VscHistory } from "react-icons/vsc";

import axios from 'axios';
import { v4 as uuidv4 } from 'uuid';
import { Lunar } from 'lunar-javascript';
import { clsx } from "clsx";

import TextareaAutosize from 'react-textarea-autosize';

type Props = Popup<{
    showingCalendar: Date,
    now: Date,
    day: Day,
    holidayInf: {
        isHoliday: boolean;
        specdays: SpecialDay[];
    };
    refresh: () => Promise<void>;
    getDayName: (date: Date, isHoliday: boolean) => React.ReactNode;
    setAllowBgClose: (allow: boolean) => void;
    sessionId: string;
    backend: string;
}>;

export default function EditSchedule({ showingCalendar, now, day, holidayInf, getDayName, refresh, close, setAllowBgClose, sessionId, backend }: Props) {
    const [ daypopup_loading, set_daypopup_loading ] = useState<boolean>(false);
    const [ daypopup_button_active, set_daypopup_button_active ] = useState<boolean>(false);

    const [ schedules, setSchedules ] = useState<Schedule[]>(day.schedules);

    useEffect(() => {
        setSchedules(day.schedules);
    }, [day]);

    const changeSchedules: (t: Schedule[]) => void = (t) => {
        // 로딩 중(서버로 데이터 업로드 중)에는 로컬 데이터가 변경되면 안됨
        if (!daypopup_loading) {
            setSchedules(() => {
                // 스케줄 칸이 비어있는 경우 (negative) 무조건 disable
                if (t.some(s => (s.content === ""))) {
                    set_daypopup_button_active(false);
                    return t;
                }

                const prevIds = day.schedules.map(d => d.id);
                const tIds = t.map(d => d.id);

                const isContentChanged = t.some(s => {
                    const originalContent = day.schedules.find(os => os.id === s.id)?.content;
                    return originalContent !== s.content;
                });

                const isChanged = isContentChanged || prevIds.length !== tIds.length || prevIds.some((id, idx) => id != tIds[idx]);

                set_daypopup_button_active(isChanged);
                return t;
            });
        }
    }

    const onSave = async () => {
        if (daypopup_button_active) {
            // 적용버튼 비활성화 및 로딩 인디케이터 활성화
            setAllowBgClose(false);
            set_daypopup_button_active(false);
            set_daypopup_loading(true);

            // Test: await new Promise(resolve => setTimeout(resolve, 5000));

            try {
                await axios.post(backend + "/webservice/setSchedules", {
                    date: day.date,
                    schedules,
                    bgColor: "",
                }, {
                    headers: {
                        'X-Client-Token': localStorage.getItem("calisync_token"),
                        'X-Client-ID': sessionId
                    }
                });

                close();
                setAllowBgClose(true);
                set_daypopup_loading(false);
                await refresh();
            } catch (err) {
                console.error(err);
                setAllowBgClose(true);
                set_daypopup_button_active(false);
                set_daypopup_loading(false);
                return;
            }
        }
    }

    const onClose = () => {
        // 로딩 중에는 팝업 닫기 불가
        if (!daypopup_loading) {
            close();
            setSchedules(day.schedules);
            set_daypopup_button_active(false);
        }
    }

    const currentDay = parseInt(day.date.slice(-2));

    return (
        <div className="w-full h-full flex flex-col font-suite text-white">
            <div className={"flex flex-col grow"}>
                <span className="mx-auto mb-3 block text-center">일정 수정</span>
                <div className="text-[1.5rem] -mt-2 -mb-1 text-gray-300">
                    {showingCalendar.getFullYear()}년
                </div>

                <div className={"flex items-end"}>
                    <span className="text-[2rem]">
                        {showingCalendar.getMonth() + 1}월
                        <span className="ml-2">{currentDay}일</span>
                    </span>
                    {(() => {
                        const date = new Date(
                            showingCalendar.getFullYear(),
                            showingCalendar.getMonth(),
                            currentDay
                        );
                        const lunar = Lunar.fromDate(date);
                        return (
                            <div className="ml-2 pb-1.5 text-gray-400 flex items-end">
                                {getDayName(date, holidayInf.isHoliday)}
                                <span className="mx-1">·</span>
                                <span className={"text-[0.9rem] mr-1"}>(음)</span>
                                {/*해당 달이 윤달일 경우 월 앞에 -를 붙이고 나오기 때문에 무조건 +가 되도록 변경*/}
                                {Math.abs(lunar.getMonth())}월 {lunar.getDay()}일
                            </div>
                        );
                    })()}
                </div>

                <div
                    className={clsx(
                        holidayInf.specdays.length !== 0 &&
                        "flex flex-wrap mt-1 min-h-5 max-h-12 overflow-y-scroll gap-2"
                    )}
                >
                    {now.getFullYear() === showingCalendar.getFullYear() &&
                        now.getMonth() === showingCalendar.getMonth() &&
                        now.getDate() === currentDay && (
                            <div className="px-2 h-5 text-[0.9rem] rounded bg-blue-900 font-bold inline-block truncate">
                                오늘
                            </div>
                        )}

                    {holidayInf.specdays.map((i, idx) => (
                        <div
                            key={idx}
                            className={clsx(
                                "px-2 h-5 text-[0.9rem] rounded truncate",
                                i.type === "holi" && "bg-red-700 text-red-100 font-bold",
                                i.type === "rest" && "bg-red-500 font-bold",
                                i.type === "anni" && "bg-purple-400 text-black",
                                i.type === "tfst" && "bg-[#F9A825]",
                                i.type === "other" && "bg-gray-400 text-black"
                            )}
                        >
                            {i.name}
                        </div>
                    ))}
                </div>

                <div className="w-full border-b border-neutral-600 my-2" />

                <div className="flex justify-between">
                    <div className="flex gap-2">
                        <div className="p-2 rounded bg-neutral-500" onClick={() => {
                            const to: Schedule[] = Array.from(schedules);

                            const month: string = ((showingCalendar.getMonth() + 1).toString().length == 1) ? "0" + (showingCalendar.getMonth() + 1) : (showingCalendar.getMonth() + 1).toString();
                            const day: string = (currentDay.toString().length == 1) ? "0" + (currentDay.toString()) : currentDay.toString();

                            to.push({
                                id: uuidv4(),
                                date: `${showingCalendar.getFullYear()}${month}${day}`,
                                content: "",
                                isCompleted: false,
                            });
                            changeSchedules(to);
                        }}><IoAddOutline /></div>
                        <div className="p-2 rounded bg-neutral-500"><VscHistory /></div>
                        <div className="p-2 rounded bg-neutral-500"><RiDeleteBin6Line /></div>
                    </div>
                    <div className="border border-gray-400 rounded overflow-hidden">
                        <img src={no_color} alt="nc" className="w-7 h-7" />
                    </div>
                </div>

                <div className="w-full grow">
                    <Reorder.Group
                        layout
                        axis="y"
                        values={schedules}
                        onReorder={changeSchedules}
                        className={clsx("mt-4 overflow-y-scroll h-full")}
                        style={{ scrollbarWidth: "none" }}
                    >
                        <AnimatePresence mode="popLayout">
                            {schedules.length === 0 ? (
                                <div className="mt-20 text-center text-gray-400">
                                    {daypopup_loading ? "로딩 중입니다..." : "이 날은 일정 및 이벤트가 없습니다."}
                                </div>
                            ) : schedules.map(item => (
                                    <ScheduleItem
                                        key={item.id}
                                        itemId={item.id}
                                        schedules={schedules}
                                        setSchedules={changeSchedules}
                                    />
                                ))
                            }
                        </AnimatePresence>
                    </Reorder.Group>
                </div>
            </div>

            <div className="flex my-5">
                <div
                    className="w-1/2 h-12 flex items-center justify-center rounded-lg bg-neutral-500 border border-gray-600"
                    onPointerUp={onClose}
                >
                    취소
                </div>
                <div
                    className={clsx(
                        "w-1/2 h-12 ml-5 flex items-center justify-center rounded-lg",
                        "transition-colors duration-300",
                        daypopup_button_active ? "bg-blue-500" : "bg-neutral-600"
                    )}
                    onPointerUp={onSave}
                >
                    {
                        daypopup_loading ? (
                            <div className={"w-5 h-5 border-2 border-white/20 border-t-white/40 rounded-full animate-spin"}>
                            </div>
                        ) : "저장"
                    }
                </div>
            </div>
        </div>
    );
}

const ScheduleItem = ({ itemId, schedules, setSchedules }: { itemId: string; schedules: Schedule[]; setSchedules: (t: Schedule[]) => void; }) => {
    const reorderItemDragControl = useDragControls();
    const deleteItemDragControl = useDragControls();
    const deleteItemAnimation = useAnimation();

    const item = schedules.find(item => item.id === itemId) || null;

    return (
        <Reorder.Item
            value={item}
            layout
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{
                opacity: 0,
                height: 0,
                marginBottom: 0,
                paddingTop: 0,
                paddingBottom: 0
            }}
            dragListener={ false }
            dragControls={ reorderItemDragControl }
            transition={{ duration: 0.2 }}
            className="relative mb-2 list-none touch-none overflow-hidden shrink-0"
        >
            <div className={"absolute flex justify-end items-center pr-2 z-0 inset-0 bg-red-400 rounded my-0.5 left-10"}>
                <RiDeleteBin6Line />
            </div>
            <motion.div
                drag={"x"}
                dragConstraints={{ left: -70, right: 0 }}
                dragElastic={{ left: 0.5, right: 0 }}
                onDragEnd={async (_, info) => {
                    if (info.offset.x <= -100) {
                        await deleteItemAnimation.start({
                            x: -300,
                            transition: { duration: 0.2 }
                        });
                        setSchedules(schedules.filter(p => p.id !== item?.id));
                    } else if (info.offset.x < -15) {
                        await deleteItemAnimation.start({
                            x: -30,
                            transition: { duration: 0.2 }
                        });
                    } else {
                        await deleteItemAnimation.start({
                            x: 0,
                            transition: { duration: 0.2 }
                        });
                    }
                }}
                animate={ deleteItemAnimation }
                dragControls={ deleteItemDragControl }
                className={"relative flex z-10 w-full items-center bg-neutral-800 border border-gray-600 rounded"}
            >
                <div className={"text-[1.1rem] text-gray-400 mx-2"} onPointerUp={(e) => {
                    e.stopPropagation();
                    reorderItemDragControl.start(e);
                }}>
                    <RxDragHandleDots2/>
                </div>
                <div className={"w-[90%] py-2 pr-4 touch-none wrap-break-word"} onPointerUp={(e) => deleteItemDragControl.start(e)}>
                    <TextareaAutosize
                        value={item?.content}
                        spellCheck={false}
                        className={"outline-none w-full bg-transparent wrap-break-word border-none resize-none overflow-hidden text-inherit font-inherit py-0 m-0 leading-tight block"}
                        // 한 개의 스케줄에서 사용자가 줄바꿈을 하면 의도치 않은 동작이 발생할 수 있음
                        onKeyDown={(e) => {
                            if (e.key === "Enter") e.preventDefault();
                        }}
                        onChange={(e) => {
                            const filteredValue = e.target.value.replace(/r?\n|\r/g, "");
                            const newSchedules = schedules.map(s => s.id === item?.id ? { ...s, content: filteredValue } : s);
                            setSchedules(newSchedules);
                        }
                    }/>
                </div>
            </motion.div>
        </Reorder.Item>
    );
};
