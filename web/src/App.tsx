import * as React from 'react';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import CssBaseline from '@mui/material/CssBaseline';
import FormLabel from '@mui/material/FormLabel';
import FormControl from '@mui/material/FormControl';
import Link from '@mui/material/Link';
import TextField from '@mui/material/TextField';
import ToggleButton from '@mui/material/ToggleButton';
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup';
import Typography from '@mui/material/Typography';
import Stack from '@mui/material/Stack';
import { PaletteMode, Card as MuiCard } from '@mui/material';
import { ThemeProvider, createTheme, styled } from '@mui/material/styles';

import AutoAwesomeRoundedIcon from '@mui/icons-material/AutoAwesomeRounded';

interface ToggleCustomThemeProps {
  showCustomTheme: Boolean;
  toggleCustomTheme: () => void;
}

function ToggleCustomTheme({
                             showCustomTheme,
                             toggleCustomTheme,
                           }: ToggleCustomThemeProps) {
  return (
      <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            width: '100dvw',
            position: 'fixed',
            bottom: 24,
          }}
      >
        <ToggleButtonGroup
            color="primary"
            exclusive
            value={showCustomTheme}
            onChange={toggleCustomTheme}
            aria-label="Toggle design language"
            sx={{
              backgroundColor: 'background.default',
              '& .Mui-selected': {
                pointerEvents: 'none',
              },
            }}
        >
          <ToggleButton value>
            <AutoAwesomeRoundedIcon sx={{ fontSize: '20px', mr: 1 }} />
            Custom theme
          </ToggleButton>
          <ToggleButton value={false}>Material Design 2</ToggleButton>
        </ToggleButtonGroup>
      </Box>
  );
}

const Card = styled(MuiCard)(({ theme }) => ({
  display: 'flex',
  flexDirection: 'column',
  alignSelf: 'center',
  gap: theme.spacing(4),
  width: '100%',
  padding: theme.spacing(2),
  boxShadow:
      theme.palette.mode === 'light'
          ? 'hsla(220, 30%, 5%, 0.05) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.05) 0px 15px 35px -5px, hsla(220, 30%, 5%, 0.05) 0px 0px 0px 1px'
          : 'hsla(220, 30%, 5%, 0.5) 0px 5px 15px 0px, hsla(220, 25%, 10%, 0.08) 0px 15px 35px -5px, hsla(220, 30%, 5%, 0.05) 0px 0px 0px 1px',
  [theme.breakpoints.up('sm')]: {
    padding: theme.spacing(4),
    width: '450px',
  },
}));

const SignInContainer = styled(Stack)(({ theme }) => ({
  height: 'auto',
  padingBottom: theme.spacing(12),
  backgroundImage:
      theme.palette.mode === 'light'
          ? 'radial-gradient(ellipse at 50% 50%, hsl(210, 100%, 97%), hsl(0, 0%, 100%))'
          : 'radial-gradient(at 50% 50%, hsla(210, 100%, 16%, 0.3), hsl(220, 30%, 5%))',
  backgroundRepeat: 'no-repeat',
  [theme.breakpoints.up('sm')]: {
    paddingBottom: 0,
    height: '100dvh',
  },
}));

export default function SignIn() {
  const [mode, setMode] = React.useState<PaletteMode>('light');
  const [showCustomTheme, setShowCustomTheme] = React.useState(true);
  const defaultTheme = createTheme({ palette: { mode } });
  const [emailError, setEmailError] = React.useState(false);
  const [emailErrorMessage, setEmailErrorMessage] = React.useState('');
  const [passwordError, setPasswordError] = React.useState(false);
  const [passwordErrorMessage, setPasswordErrorMessage] = React.useState('');
  const [open, setOpen] = React.useState(false);

  const toggleColorMode = () => {
    setMode((prev) => (prev === 'dark' ? 'light' : 'dark'));
  };

  const toggleCustomTheme = () => {
    setShowCustomTheme((prev) => !prev);
  };

  const handleClickOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    console.log({
      email: data.get('email'),
      password: data.get('password'),
    });
  };

  const validateInputs = () => {
    const email = document.getElementById('email') as HTMLInputElement;
    const password = document.getElementById('password') as HTMLInputElement;

    let isValid = true;

    if (!email.value || !/\S+@\S+\.\S+/.test(email.value)) {
      setEmailError(true);
      setEmailErrorMessage('Please enter a valid email address.');
      isValid = false;
    } else {
      setEmailError(false);
      setEmailErrorMessage('');
    }

    if (!password.value || password.value.length < 6) {
      setPasswordError(true);
      setPasswordErrorMessage('Password must be at least 6 characters long.');
      isValid = false;
    } else {
      setPasswordError(false);
      setPasswordErrorMessage('');
    }

    return isValid;
  };

  return (
      <ThemeProvider theme={defaultTheme}>
        <CssBaseline />
        <SignInContainer direction="column" justifyContent="space-between">
          <Stack
              justifyContent="center"
              sx={{ height: { xs: '100%', sm: '100dvh' }, p: 2 }}
          >
            <Card>
              <Typography
                  component="h1"
                  variant="h4"
                  sx={{ width: '100%', fontSize: 'clamp(2rem, 10vw, 2.15rem)' }}
              >
                Sign in
              </Typography>
              <Box
                  component="form"
                  onSubmit={handleSubmit}
                  noValidate
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    width: '100%',
                    gap: 2,
                  }}
              >
                <FormControl>
                  <FormLabel htmlFor="email">Email</FormLabel>
                  <TextField
                      error={emailError}
                      helperText={emailErrorMessage}
                      id="email"
                      type="email"
                      name="email"
                      placeholder="your@email.com"
                      autoComplete="email"
                      autoFocus
                      required
                      fullWidth
                      variant="outlined"
                      color={emailError ? 'error' : 'primary'}
                      sx={{ ariaLabel: 'email' }}
                  />
                </FormControl>
                <FormControl>
                  <Box
                      sx={{
                        display: 'flex',
                        justifyContent: 'space-between',
                      }}
                  >
                    <FormLabel htmlFor="password">Password</FormLabel>
                    <Link
                        component="button"
                        onClick={handleClickOpen}
                        variant="body2"
                        sx={{ alignSelf: 'baseline' }}
                    >
                      Forgot your password?
                    </Link>
                  </Box>
                  <TextField
                      error={passwordError}
                      helperText={passwordErrorMessage}
                      name="password"
                      placeholder="••••••"
                      type="password"
                      id="password"
                      autoComplete="current-password"
                      autoFocus
                      required
                      fullWidth
                      variant="outlined"
                      color={passwordError ? 'error' : 'primary'}
                  />
                </FormControl>
                <Button
                    type="submit"
                    fullWidth
                    variant="contained"
                    onClick={validateInputs}
                >
                  Sign in
                </Button>
              </Box>
            </Card>
          </Stack>
        </SignInContainer>
      </ThemeProvider>
  );
}