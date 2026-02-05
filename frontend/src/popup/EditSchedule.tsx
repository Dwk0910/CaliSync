import * as React from 'react';
import { useState, useEffect, useRef } from 'react';

import no_color from '../assets/editscheudle_popup_asset/no_color.png';

import { type SpecialDay, type Schedule, type Day } from "../App";
import { type Popup } from './Popup';

import { Reorder, motion, useDragControls, useAnimation, AnimatePresence } from 'framer-motion';

import { Lunar } from 'lunar-javascript';
import { clsx } from "clsx";

import { IoAddOutline } from 'react-icons/io5';
import { RxDragHandleDots2 } from "react-icons/rx";
import { RiDeleteBin6Line } from "react-icons/ri";
import { VscHistory } from "react-icons/vsc";

type Props = Popup<{
    showingCalendar: Date,
    now: Date,
    day: Day,
    holidayInf: {
        isHoliday: boolean;
        specdays: SpecialDay[];
    };
    getDay: (date: Date, isHoliday: boolean) => React.ReactNode;
}>;

export default function EditSchedule({ showingCalendar, now, day, holidayInf, getDay, close }: Props) {
    const [ daypopup_button_active ] = useState<boolean>(false);
    const [ schedules, setSchedules ] = useState<Array<{ id: string, schedule: Schedule }>>();
    const reorderGroup = useRef(null);

    // 변하는 기념일 뱃지 컨테이너 height에 따라 scheduleConainter max height도 바뀌어야 아래 버튼에 영향이 안감
    const [ scheduleContainerMaxH, setScheduleContainerMaxH ] = useState<string>("max-h-95");
    const listRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        (async () => {
            const el = listRef.current;
            if (!el) return;

            // 기념일 뱃지 컨테이너가 2줄 이상으로 길어졌을 경우
            if (el.clientHeight > 20) setScheduleContainerMaxH("max-h-75")
            else if (el.clientHeight != 0) setScheduleContainerMaxH("max-h-83")
        })()
    }, [holidayInf]);
    
    useEffect(() => {
        (async () => {
            // Day Schedules에 id 추가 (for drag function)
            const schedules_to: Array<{ id: string, schedule: Schedule }> = [];
            day.schedules.map((item, idx) => {
                schedules_to.push({
                    id: `id-${item.content}-${idx}`,
                    schedule: item
                })
            });

            setSchedules(schedules_to);
        })()
    }, [day]);

    const currentDay = parseInt(day.date.slice(-2));


    // Schedule Setting Popup (Day popup)
    return (
        <div className={"w-full h-full flex flex-col font-suite text-white justify-between"}>
            <div className={"flex flex-col"}>
                <span className={"mx-auto mb-3 text-white text-[1rem]"}>일정 수정</span>
                <span className={"text-[1.5rem] -mt-2 -mb-1 text-gray-300"}>{ showingCalendar.getFullYear() }년</span>
                <div>
                    <span className={"text-[2rem]"}>
                        <span>{ showingCalendar.getMonth() + 1 }월</span>
                        <span className={"ml-2"}>{ currentDay }일</span>
                    </span>
                    {(() => {
                        const date = new Date(showingCalendar.getFullYear(), showingCalendar.getMonth(), currentDay);
                        const lunar = Lunar.fromDate(date);
                        return (
                            <span className={"ml-3 text-gray-400"}>{ getDay(date, holidayInf.isHoliday) }<span className={"mx-2"}>·</span>(음) { lunar.getMonth() }월 { lunar.getDay() }일</span>
                        )
                    })()}
                </div>
                <div
                    ref={listRef}
                    className={clsx(
                    holidayInf.specdays.length !== 0 && "flex flex-wrap mt-1 min-h-5 max-h-12 overflow-y-scroll"
                )}>
                    {now.getFullYear() == showingCalendar.getFullYear() && now.getMonth() == showingCalendar.getMonth() && now.getDate() == currentDay && (
                        <div className={"inline-block px-2 h-5 text-[0.9rem] rounded-[5px] bg-blue-900 text-white font-bold"}>
                            오늘
                        </div>
                    )}
                    {holidayInf.specdays.map((i, idx) => {
                        return (
                            <div key={`specialday-${idx}`} className={
                                clsx(
                                    "inline-block px-2 h-5 text-[0.9rem] rounded-[5px]",
                                    "truncate",
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
                <div className={"w-full border-b border-neutral-600 my-2"}/>
                <div className={"w-full flex justify-between"}>
                    <div className={"flex gap-2"}>
                        <div className={"inline-flex items-center justify-center p-2 rounded-[5px] bg-neutral-500 text-[1.2rem]"}><IoAddOutline /></div>
                        <div className={"inline-flex items-center justify-center p-2 rounded-[5px] bg-neutral-500"}><VscHistory /></div>
                        <div className={"inline-flex items-center justify-center p-2 rounded-[5px] bg-neutral-500"}><RiDeleteBin6Line /></div>
                    </div>
                    <div className={"flex items-center justify-center"}>
                        <div className={"border border-gray-400 rounded-[5px] overflow-hidden"}>
                            <img src={no_color} alt="n" className={"w-7 h-7 -p-1"}/>
                        </div>
                    </div>
                </div>

                <div className={"w-full"}>
                    {schedules?.length === 0 ? (
                        <div className={clsx(
                            "w-full flex justify-center mt-4",
                        )} style={{ scrollbarWidth: "none" }}>
                            <span className={"pb-2 text-gray-400 text-center mt-20"}>
                                이 날은 일정 및 이벤트가 없습니다.
                            </span>
                        </div>
                    ) : (
                        <motion.div layout>
                            <Reorder.Group
                                axis={"y"}
                                ref={reorderGroup}
                                values={schedules!}
                                onReorder={setSchedules}
                                className={clsx("mt-4 flex flex-col gap-2 overflow-y-scroll", scheduleContainerMaxH)}
                                style={{ scrollbarWidth: 'none' }}
                            >
                                <AnimatePresence mode={"popLayout"}>
                                    {schedules?.map((item) => (
                                        <ScheduleItem reorderGroup={reorderGroup} handleDelete={(item) => {
                                            setSchedules((prev) => prev?.filter(prev => prev.id !== item.id));
                                        }} item={item} key={`scheduleitem-${item.id}`} />
                                    ))}
                                </AnimatePresence>
                            </Reorder.Group>
                        </motion.div>
                    )}
                </div>

            </div>
            <div className={"flex my-5"}>
                <div className={clsx(
                    "flex justify-center items-center w-[50%] h-12 rounded-lg",
                    "transition-all duration-200 ease-in-out border border-gray-600",
                    "bg-neutral-500"
                )} onClick={() => close()}>
                    <span className={"font-suite text-xl"}>취소</span>
                </div>
                <div className={clsx(
                    "flex justify-center items-center ml-5 w-[50%] h-12 rounded-lg",
                    "transition-all duration-200 ease-in-out",
                    daypopup_button_active ? "bg-green-600/90" : "bg-neutral-600")
                } onClick={() => {
                    if (daypopup_button_active) return;
                    close();
                }}>
                    <span className={"font-suite text-xl"}>저장</span>
                </div>
            </div>
        </div>
    );
}

// 스케줄 아이템 (useDragControls를 사용하기 위해 외부에 선언)
const ScheduleItem = ({ reorderGroup, handleDelete, item }: { reorderGroup: React.RefObject<null>; handleDelete: (item: { id: string; schedule: Schedule; }) => void; item: { id: string; schedule: Schedule; } }) => {
    const i = item.schedule;
    const dragControl = useDragControls();

    const motionDivAnimationControl = useAnimation();
    const reorderItemAniamtionControl = useAnimation();

    return (
        <Reorder.Item
            layout
            value={item}
            animate={ reorderItemAniamtionControl }
            drag={"y"}
            dragConstraints={reorderGroup}
            dragElastic={0.15}
            dragListener={false}
            dragControls={dragControl}
            className={"w-full relative shrink-0"}
        >
            <div className={"w-full shrink-0"}>
                <div className={"absolute inset-y-0 left-10 right-0 pr-2 py-2 my-0.5 bg-red-400 flex justify-end items-center rounded-[5px] z-0"}>
                    <RiDeleteBin6Line/>
                </div>
                <motion.div
                    drag={"x"}
                    initial={{ x: 0 }}
                    animate={ motionDivAnimationControl }
                    dragConstraints={{ left: -50, right: 0 }}
                    dragElastic={{ left: 0.5, right: 0 }}
                    onDragEnd={async (_, info) => {
                        // 터치를 땠을 때 x offset이 -100이 넘어갈 경우 (충분히 삭제 쪽으로 스와이프 했을 때)
                        if (info.offset.x < -200) {
                            await Promise.all([
                                motionDivAnimationControl.start({ x: -300, opacity: 0 }),
                                reorderItemAniamtionControl.start({
                                    height: 0,
                                    opacity: 0,
                                    marginTop: 0,
                                    marginBottom: -8,
                                    paddingTop: 0,
                                    paddingBottom: 0,
                                    transition: { duration: 0.3, ease: "easeInOut" }
                                }),
                            ]);

                            handleDelete(item);
                        } else if (info.offset.x <= -25) {
                            void motionDivAnimationControl.start({
                                x: -50,
                                transition: { type: "spring", stiffness: 500, damping: 30 }
                            });
                        } else {
                            void motionDivAnimationControl.start({
                                x: 0,
                                transition: { type: "spring", stiffness: 500, damping: 30 }
                            });
                        }
                    }}
                    className={clsx(
                    "bg-neutral-800 p-2 pl-4 overflow-x-hidden text-wrap line-clamp-2 flex items-center rounded-[5px] border border-gray-600",
                    "flex z-10 relative"
                    )}
                >
                    <div className={"w-[5%] pl-1 pr-5 flex justify-center touch-none"} onPointerDown={(e) => {
                        e.stopPropagation();
                        dragControl.start(e);
                    }}>
                        <span className={"text-[1.7rem] text-gray-400"}><RxDragHandleDots2 size={23}/></span>
                    </div>
                    <span className={"w-[95%] pr-2 text-wrap wrap-break-word"}>{i.content}</span>
                </motion.div>
            </div>
        </Reorder.Item>
    );
}
