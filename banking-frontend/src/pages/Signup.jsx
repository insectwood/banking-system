import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { authApi } from '../api/api';

const Signup = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const handleSignup = async (e) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            await authApi.post('/signup', {
                email,
                password,
                name,
            });

            alert('Sign-up completed! Please log in.');
            navigate('/login');
        } catch (error) {
            const errorMsg = error.response?.data?.message || 'An error occurred during sign-up.';
            alert(errorMsg);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={styles.container}>
            <div style={styles.signupBox}>
                <h2>Create Account</h2>
                <p>Register for Banking SSO</p>
                <form onSubmit={handleSignup} style={styles.form}>
                    <input
                        type="text"
                        placeholder="Enter Name"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        style={styles.input}
                        required
                    />
                    <input
                        type="email"
                        placeholder="Enter Email"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        style={styles.input}
                        required
                    />
                    <input
                        type="password"
                        placeholder="Enter Password"
                        value={password}
                        onChange={(e) => setPassword(e.target.value)}
                        style={styles.input}
                        required
                        minLength={8}
                    />
                    <button type="submit" disabled={isLoading} style={styles.button}>
                        {isLoading ? 'Creating...' : 'Sign Up'}
                    </button>
                </form>
                <p style={styles.loginLink}>
                    Already have an account?{' '}
                    <span onClick={() => navigate('/login')} style={styles.link}>Log in</span>
                </p>
            </div>
        </div>
    );
};

const styles = {
    container: { display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh', backgroundColor: '#f0f2f5' },
    signupBox: { padding: '40px', backgroundColor: '#fff', borderRadius: '8px', boxShadow: '0 4px 12px rgba(0,0,0,0.1)', textAlign: 'center', minWidth: '360px' },
    form: { display: 'flex', flexDirection: 'column', gap: '15px', marginTop: '20px' },
    input: { padding: '12px', borderRadius: '4px', border: '1px solid #ddd', fontSize: '16px' },
    button: { padding: '12px', backgroundColor: '#2ecc71', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer', fontSize: '16px' },
    loginLink: { marginTop: '20px', fontSize: '14px', color: '#666' },
    link: { color: '#007bff', cursor: 'pointer', textDecoration: 'underline' },
};

export default Signup;
