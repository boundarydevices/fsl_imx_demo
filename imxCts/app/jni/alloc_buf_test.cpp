/*
 * Copyright (C) 2010-2016 Freescale Semiconductor, Inc. All Rights Reserved.
 */

/*
 * The code contained herein is licensed under the GNU General Public
 * License. You may obtain a copy of the GNU General Public License
 * Version 2 or later at the following locations:
 *
 * http://www.opensource.org/licenses/gpl-license.html
 * http://www.gnu.org/copyleft/gpl.html
 */

#include "util_alloc_buf.h"

#define MAX_BUF 256

int xsize=0;
int ysize=0;


int change_oom_adj_score(int oom_val){
    char oom_adj_buf[MAX_BUF] = {0};
    int fd;
    char ch[6] = {0,0,0,0,0,0};

    snprintf(oom_adj_buf, sizeof(oom_adj_buf), "/proc/%d/oom_score_adj",getpid());
    INFO( "Process id: %d\n", getpid());

    int val_orig=0;
    fd = open(oom_adj_buf, O_RDWR);
    if (fd < 0) {
        return -1;
    }
    read(fd, ch, 6);
    val_orig = atof(ch);
    INFO("%s orginal val is %d \n", oom_adj_buf, val_orig);

    char numStr[6];
    bzero(numStr,6);
    snprintf(numStr,5,"%d",oom_val);
    INFO("want to change oom_score_adj to %s \n", numStr);
    write(fd, numStr, 5);
    close(fd);

    int val_check=0;
    fd = open(oom_adj_buf, O_RDONLY);
    read(fd, ch, 6);
    val_check = atof(ch);
    INFO("%s val now is %d \n", oom_adj_buf, val_check);
    close(fd);

    return 0;
}


static void usage(const char *argv0)
{
    printf("Usage: %s [options]\n", argv0);
    printf("-d, --sleep-time ****ms  time your can type 0~100000\n");
    printf("-h, --help             Show this help screen\n");
    printf("-o, --oom_score_adj  -1000~1000   change this process oom_score_adj value ;defalut is 0\n");
}

static struct option opts[] = {
    { "sleep-time", required_argument, NULL, 't' },
    { "help", no_argument, NULL, 'h'},
    { "oom_score_adj", required_argument, NULL, 'o' },
    { NULL, 0, NULL, 0 }
};

int test(int argc, char *argv[])
{
    int ret = 0;
    Test_alloc_Window *win;
    int oom_adj_score=0;

    /* Options parsing */
    int c;

    win = static_cast<Test_alloc_Window*> (malloc(sizeof(Test_alloc_Window)));
    if(!win)
        return 0;
    memset(win, 0, sizeof(Test_alloc_Window));

    /* Parse command line arguments */
    while ((c = getopt_long(argc, argv, "t:ho:", opts, NULL)) != -1) {
        switch (c) {
            case 't':
                win->alloc_time = atof(optarg);
                break;
            case 'h':
                usage(argv[0]);
                free(win);
                return 0;
            case 'o':
                oom_adj_score = atof(optarg);
                break;
            default:
                printf("Invalid option -%c\n", c);
                printf("Run %s -h for help\n", argv[0]);
                free(win);
                return 0;
        }
    }

    INFO("sleep for %d ms between alloc buffer. \n", win->alloc_time);
    change_oom_adj_score(oom_adj_score);
    test_gralloc_random(win);
    return 0;
}



