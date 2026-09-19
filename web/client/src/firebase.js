import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider, signInWithPopup, signOut } from 'firebase/auth';

const firebaseConfig = {
  apiKey: "AIzaSyCVmtVLhUak1338utDMdyNXBcrprBBmXOg",
  authDomain: "abbattleships.firebaseapp.com",
  projectId: "abbattleships",
  storageBucket: "abbattleships.firebasestorage.app",
  messagingSenderId: "498836539028",
  appId: "1:498836539028:web:58dabb6db4736e796f1681",
  measurementId: "G-3WCD0XE4W8"
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const provider = new GoogleAuthProvider();

export const signInWithGoogle = async () => {
  try {
    const result = await signInWithPopup(auth, provider);
    return result.user;
  } catch (error) {
    console.error("Google Sign-In Error:", error);
    throw error;
  }
};

export const logOut = async () => {
  try {
    await signOut(auth);
  } catch (error) {
    console.error("Logout Error:", error);
    throw error;
  }
};

export { auth };
