import axios from "axios";
import { useGoogleLogin } from '@react-oauth/google';

// useGoogleLogin 훅은 App 안에서 정의할 수 없음 (훅들은 App이 불려오면서 실행되는데, useGoogleLogin은 <GoogleOAuthProvider/> 안에 속해야 하기 때문에 외부에서 선언한 다음 App에서 호출해야함
export default function LoginButtion({ backend }: { backend: string; }) {
    const login = useGoogleLogin({
        flow: 'implicit',
        scope: 'openid email profile',
        onSuccess: (res) => {
            axios.post(backend + "/auth/getToken", { google_token: res.access_token }).then((res) => {
                localStorage.setItem("calisync_token", res.data);
                window.location.reload();
            });
        }
    });

    return (
        <button className={"font-suite text-white border border-neutral-500 p-2 scale-70"} onPointerUp={() => login()}>
            <span className={"font-bold"}>Google 계정</span>
            <span className={"text-neutral-300"}>으로 인증하기</span>
        </button>
    )
}